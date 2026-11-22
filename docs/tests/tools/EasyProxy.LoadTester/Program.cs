using System.Collections.Concurrent;
using System.Diagnostics;
using System.Globalization;
using System.IO.Compression;
using System.Net;
using System.Net.Http.Headers;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Threading.Channels;

namespace EasyProxy.LoadTester;

internal static class Program
{
    private static async Task<int> Main(string[] args)
    {
        if (args.Any(static value => value is "--help" or "-h"))
        {
            Console.WriteLine(Options.HelpText);
            return 0;
        }

        Options options;
        try
        {
            options = Options.Parse(args);
            options.Validate();
        }
        catch (Exception exception)
        {
            Console.Error.WriteLine($"ARGUMENT_ERROR={exception.Message}");
            Console.Error.WriteLine(Options.HelpText);
            return 2;
        }

        using var cancellation = new CancellationTokenSource();
        string stopReason = "completed";
        ConsoleCancelEventHandler cancelHandler = (_, eventArgs) =>
        {
            eventArgs.Cancel = true;
            stopReason = "cancelled";
            cancellation.Cancel();
        };
        Console.CancelKeyPress += cancelHandler;

        try
        {
            var fixtures = FixtureCatalog.Load(options.ManifestPath);
            var result = await LoadRunner.RunAsync(options, fixtures, cancellation.Token, reason => stopReason = reason);
            Console.WriteLine($"SUMMARY_PATH={result.SummaryPath}");
            Console.WriteLine($"REQUESTS_PATH={result.RequestsPath}");
            Console.WriteLine($"HEALTH_PATH={result.HealthPath}");
            Console.WriteLine($"TOTAL_REQUESTS={result.TotalRequests}");
            Console.WriteLine($"SUCCESS_RATE={result.SuccessRate.ToString("F6", CultureInfo.InvariantCulture)}");
            Console.WriteLine($"STOP_REASON={result.StopReason}");
            Console.WriteLine($"STATUS={(result.Passed ? "PASS" : "FAIL")}");
            foreach (var failure in result.Failures)
            {
                Console.WriteLine($"THRESHOLD_FAILURE={failure}");
            }
            return result.Passed ? 0 : result.StopReason == "cancelled" ? 130 : 1;
        }
        catch (OperationCanceledException)
        {
            Console.WriteLine($"STATUS=CANCELLED");
            Console.WriteLine($"STOP_REASON={stopReason}");
            return 130;
        }
        catch (Exception exception)
        {
            Console.Error.WriteLine($"LOAD_TEST_ERROR={exception.GetType().Name}: {exception.Message}");
            return 3;
        }
        finally
        {
            Console.CancelKeyPress -= cancelHandler;
        }
    }
}

internal sealed class Options
{
    public string BaseUrl { get; private set; } = "";
    public string ManifestPath { get; private set; } = Path.Combine("docs", "tests", "data", "manifests", "fixtures.csv");
    public string Scenario { get; private set; } = "small";
    public int Concurrency { get; private set; } = 10;
    public int WarmupSeconds { get; private set; }
    public int DurationSeconds { get; private set; } = 60;
    public int CooldownSeconds { get; private set; }
    public int TargetRps { get; private set; }
    public long TotalRequests { get; private set; }
    public string Connections { get; private set; } = "keep-alive";
    public int RequestTimeoutSeconds { get; private set; } = 30;
    public string HealthUrl { get; private set; } = "";
    public int HealthIntervalSeconds { get; private set; } = 1;
    public int HealthStopSeconds { get; private set; } = 30;
    public string OutputDir { get; private set; } = ".";
    public string FilePrefix { get; private set; } = "load";
    public double? MinSuccessRate { get; private set; }
    public double? MaxP95Ms { get; private set; }
    public double? MaxP99Ms { get; private set; }
    public double? MinHealthRate { get; private set; }
    public bool FailOn5xx { get; private set; } = true;
    public int Expected5xx { get; private set; }
    public int RandomSeed { get; private set; } = 20261122;

    public static string HelpText => """
EasyProxy.LoadTester options:
  --base-url URL                 Required target origin, for example http://68.64.181.242:28080
  --manifest PATH                fixtures.csv path (default: docs/tests/data/manifests/fixtures.csv)
  --scenario NAME                small|distinct|large|slow|mixed|connection (default: small)
  --concurrency N                Concurrent workers (default: 10)
  --warmup-seconds N             Warmup duration excluded from summary (default: 0)
  --duration-seconds N           Measured duration (default: 60)
  --cooldown-seconds N           Post-load low-rate recovery probe duration (default: 0)
  --target-rps N                 Optional aggregate requests/second cap (0 = unlimited)
  --total-requests N             Optional total request cap for measured phase (0 = unlimited)
  --connections MODE             keep-alive|close|mixed (default: keep-alive)
  --request-timeout-seconds N    Per-request timeout (default: 30)
  --health-url URL               Independent health/data probe URL (default: base URL small fixture)
  --health-interval-seconds N    Health probe interval (default: 1)
  --health-stop-seconds N        Stop when health fails continuously; 0 disables (default: 30)
  --output-dir PATH              Evidence output directory (default: current directory)
  --file-prefix NAME             Output prefix, for example T-21_c100
  --min-success-rate R           Pass threshold in [0,1]
  --max-p95-ms N                 Pass threshold for p95
  --max-p99-ms N                 Pass threshold for p99
  --min-health-rate R            Health probe availability threshold in [0,1]
  --expected-5xx N               Planned 5xx allowance (default: 0)
  --fail-on-5xx                  Enable unexpected 5xx failure (default)
  --no-fail-on-5xx               Disable 5xx failure check
  --random-seed N                Deterministic scenario selection seed
  --help                         Show this help
""";

    public static Options Parse(string[] args)
    {
        var options = new Options();
        for (var index = 0; index < args.Length; index++)
        {
            var key = args[index];
            string NextValue()
            {
                if (index + 1 >= args.Length)
                {
                    throw new ArgumentException($"Missing value for {key}");
                }
                return args[++index];
            }

            switch (key)
            {
                case "--base-url": options.BaseUrl = NextValue(); break;
                case "--manifest": options.ManifestPath = NextValue(); break;
                case "--scenario": options.Scenario = NextValue().ToLowerInvariant(); break;
                case "--concurrency": options.Concurrency = ParseInt(key, NextValue()); break;
                case "--warmup-seconds": options.WarmupSeconds = ParseInt(key, NextValue()); break;
                case "--duration-seconds": options.DurationSeconds = ParseInt(key, NextValue()); break;
                case "--cooldown-seconds": options.CooldownSeconds = ParseInt(key, NextValue()); break;
                case "--target-rps": options.TargetRps = ParseInt(key, NextValue()); break;
                case "--total-requests": options.TotalRequests = ParseLong(key, NextValue()); break;
                case "--connections": options.Connections = NextValue().ToLowerInvariant(); break;
                case "--request-timeout-seconds": options.RequestTimeoutSeconds = ParseInt(key, NextValue()); break;
                case "--health-url": options.HealthUrl = NextValue(); break;
                case "--health-interval-seconds": options.HealthIntervalSeconds = ParseInt(key, NextValue()); break;
                case "--health-stop-seconds": options.HealthStopSeconds = ParseInt(key, NextValue()); break;
                case "--output-dir": options.OutputDir = NextValue(); break;
                case "--file-prefix": options.FilePrefix = NextValue(); break;
                case "--min-success-rate": options.MinSuccessRate = ParseDouble(key, NextValue()); break;
                case "--max-p95-ms": options.MaxP95Ms = ParseDouble(key, NextValue()); break;
                case "--max-p99-ms": options.MaxP99Ms = ParseDouble(key, NextValue()); break;
                case "--min-health-rate": options.MinHealthRate = ParseDouble(key, NextValue()); break;
                case "--expected-5xx": options.Expected5xx = ParseInt(key, NextValue()); break;
                case "--fail-on-5xx": options.FailOn5xx = true; break;
                case "--no-fail-on-5xx": options.FailOn5xx = false; break;
                case "--random-seed": options.RandomSeed = ParseInt(key, NextValue()); break;
                default: throw new ArgumentException($"Unknown option: {key}");
            }
        }
        return options;
    }

    public void Validate()
    {
        if (string.IsNullOrWhiteSpace(BaseUrl) || !Uri.TryCreate(BaseUrl, UriKind.Absolute, out _))
        {
            throw new ArgumentException("--base-url must be an absolute HTTP(S) URL");
        }
        if (!File.Exists(ManifestPath))
        {
            throw new ArgumentException($"Manifest not found: {ManifestPath}");
        }
        if (Scenario is not ("small" or "distinct" or "large" or "slow" or "mixed" or "connection"))
        {
            throw new ArgumentException($"Unsupported scenario: {Scenario}");
        }
        if (Connections is not ("keep-alive" or "close" or "mixed"))
        {
            throw new ArgumentException($"Unsupported connections mode: {Connections}");
        }
        if (Concurrency is < 1 or > 4096) throw new ArgumentException("--concurrency must be 1..4096");
        if (WarmupSeconds < 0 || DurationSeconds < 1 || CooldownSeconds < 0) throw new ArgumentException("Invalid duration");
        if (TargetRps < 0 || TotalRequests < 0) throw new ArgumentException("Rate and request counts cannot be negative");
        if (RequestTimeoutSeconds < 1) throw new ArgumentException("--request-timeout-seconds must be positive");
        if (HealthIntervalSeconds < 1) throw new ArgumentException("--health-interval-seconds must be positive");
        if (HealthStopSeconds < 0) throw new ArgumentException("--health-stop-seconds cannot be negative");
        if (MinSuccessRate is < 0 or > 1) throw new ArgumentException("--min-success-rate must be 0..1");
        if (MaxP95Ms is < 0 || MaxP99Ms is < 0) throw new ArgumentException("Latency thresholds cannot be negative");
        if (MinHealthRate is < 0 or > 1) throw new ArgumentException("--min-health-rate must be 0..1");
        if (Expected5xx < 0) throw new ArgumentException("--expected-5xx cannot be negative");
        if (string.IsNullOrWhiteSpace(FilePrefix) || FilePrefix.IndexOfAny(Path.GetInvalidFileNameChars()) >= 0)
        {
            throw new ArgumentException("--file-prefix is not a safe file name");
        }
        if (string.IsNullOrWhiteSpace(HealthUrl))
        {
            HealthUrl = BaseUrl.TrimEnd('/') + "/fixtures/small-1k.bin";
        }
    }

    private static int ParseInt(string key, string value) =>
        int.TryParse(value, NumberStyles.Integer, CultureInfo.InvariantCulture, out var parsed) ? parsed : throw new ArgumentException($"{key} requires an integer");

    private static long ParseLong(string key, string value) =>
        long.TryParse(value, NumberStyles.Integer, CultureInfo.InvariantCulture, out var parsed) ? parsed : throw new ArgumentException($"{key} requires an integer");

    private static double ParseDouble(string key, string value) =>
        double.TryParse(value, NumberStyles.Float, CultureInfo.InvariantCulture, out var parsed) ? parsed : throw new ArgumentException($"{key} requires a number");
}

internal enum FixtureKind { Small, Distinct, Large, Slow }

internal sealed record Fixture(string RelativePath, long SizeBytes, string Sha256, FixtureKind Kind);

internal sealed class FixtureCatalog
{
    public required IReadOnlyList<Fixture> Fixtures { get; init; }
    public required Fixture Small { get; init; }
    public required IReadOnlyList<Fixture> Distinct { get; init; }
    public required Fixture Large { get; init; }
    public required Fixture Slow { get; init; }

    public static FixtureCatalog Load(string manifestPath)
    {
        var absoluteManifest = Path.GetFullPath(manifestPath);
        var fixturesRoot = Path.GetFullPath(Path.Combine(Path.GetDirectoryName(absoluteManifest)!, "..", "fixtures"));
        var rows = ParseCsv(File.ReadAllLines(absoluteManifest));
        if (rows.Count < 2) throw new InvalidDataException("Fixture manifest is empty");
        var header = rows[0].Select(static value => value.Trim().ToLowerInvariant()).ToArray();
        var pathIndex = Array.IndexOf(header, "relative_path");
        var sizeIndex = Array.IndexOf(header, "size_bytes");
        var hashIndex = Array.IndexOf(header, "sha256");
        if (pathIndex < 0 || sizeIndex < 0 || hashIndex < 0) throw new InvalidDataException("Fixture manifest is missing required columns");

        var fixtures = new List<Fixture>();
        foreach (var row in rows.Skip(1))
        {
            if (row.Count <= Math.Max(pathIndex, Math.Max(sizeIndex, hashIndex))) continue;
            var relativePath = row[pathIndex].Replace('\\', '/');
            var size = long.Parse(row[sizeIndex], CultureInfo.InvariantCulture);
            var hash = row[hashIndex].Trim().ToLowerInvariant();
            var localPath = Path.Combine(fixturesRoot, relativePath.Replace('/', Path.DirectorySeparatorChar));
            if (!File.Exists(localPath)) throw new FileNotFoundException($"Fixture file missing: {localPath}");
            var actualSize = new FileInfo(localPath).Length;
            if (actualSize != size) throw new InvalidDataException($"Fixture size mismatch: {relativePath}");
            var kind = relativePath.Equals("small-1k.bin", StringComparison.OrdinalIgnoreCase) ? FixtureKind.Small
                : relativePath.StartsWith("distinct/", StringComparison.OrdinalIgnoreCase) ? FixtureKind.Distinct
                : relativePath.Equals("large-10m.bin", StringComparison.OrdinalIgnoreCase) ? FixtureKind.Large
                : relativePath.Equals("slow-2s.bin", StringComparison.OrdinalIgnoreCase) ? FixtureKind.Slow
                : throw new InvalidDataException($"Unknown fixture purpose: {relativePath}");
            fixtures.Add(new Fixture(relativePath, size, hash, kind));
        }

        var small = fixtures.FirstOrDefault(static item => item.Kind == FixtureKind.Small) ?? throw new InvalidDataException("Small fixture missing");
        var distinct = fixtures.Where(static item => item.Kind == FixtureKind.Distinct).OrderBy(static item => item.RelativePath, StringComparer.Ordinal).ToArray();
        var large = fixtures.FirstOrDefault(static item => item.Kind == FixtureKind.Large) ?? throw new InvalidDataException("Large fixture missing");
        var slow = fixtures.FirstOrDefault(static item => item.Kind == FixtureKind.Slow) ?? throw new InvalidDataException("Slow fixture missing");
        if (distinct.Length != 1000) throw new InvalidDataException($"Expected 1000 distinct fixtures, found {distinct.Length}");
        return new FixtureCatalog { Fixtures = fixtures, Small = small, Distinct = distinct, Large = large, Slow = slow };
    }

    private static List<List<string>> ParseCsv(string[] lines)
    {
        var result = new List<List<string>>();
        foreach (var line in lines)
        {
            if (string.IsNullOrWhiteSpace(line)) continue;
            var row = new List<string>();
            var value = new StringBuilder();
            var quoted = false;
            for (var index = 0; index < line.Length; index++)
            {
                var character = line[index];
                if (character == '"')
                {
                    if (quoted && index + 1 < line.Length && line[index + 1] == '"')
                    {
                        value.Append('"');
                        index++;
                    }
                    else
                    {
                        quoted = !quoted;
                    }
                }
                else if (character == ',' && !quoted)
                {
                    row.Add(value.ToString());
                    value.Clear();
                }
                else
                {
                    value.Append(character);
                }
            }
            row.Add(value.ToString());
            result.Add(row);
        }
        return result;
    }
}
internal sealed record LoadResult(
    string SummaryPath,
    string RequestsPath,
    string HealthPath,
    long TotalRequests,
    double SuccessRate,
    bool Passed,
    string StopReason,
    IReadOnlyList<string> Failures);

internal sealed record RequestSample(
    string TimestampUtc,
    string Phase,
    int Worker,
    long Index,
    string Path,
    int StatusCode,
    double LatencyMs,
    long ActualBytes,
    long ExpectedBytes,
    string ActualSha256,
    string ExpectedSha256,
    bool SizeMatches,
    bool HashMatches,
    string ConnectionMode,
    bool Success,
    string ErrorType,
    string Error);

internal sealed record HealthSample(
    string TimestampUtc,
    string Phase,
    double ElapsedSeconds,
    bool Success,
    int StatusCode,
    double LatencyMs,
    string ErrorType,
    string Error);

internal static class LoadRunner
{
    private static readonly UTF8Encoding Utf8NoBom = new(false, true);

    public static async Task<LoadResult> RunAsync(
        Options options,
        FixtureCatalog fixtures,
        CancellationToken cancellationToken,
        Action<string> reportStopReason)
    {
        Directory.CreateDirectory(options.OutputDir);
        var requestsPath = Path.Combine(options.OutputDir, $"{options.FilePrefix}-requests.csv");
        var healthPath = Path.Combine(options.OutputDir, $"{options.FilePrefix}-health.csv");
        var summaryPath = Path.Combine(options.OutputDir, $"{options.FilePrefix}-summary.json");

        await using var requestsWriter = OpenWriter(requestsPath);
        await using var healthWriter = OpenWriter(healthPath);
        await requestsWriter.WriteLineAsync(RequestCsvHeader);
        await healthWriter.WriteLineAsync(HealthCsvHeader);

        var requestChannel = Channel.CreateBounded<RequestSample>(new BoundedChannelOptions(65536)
        {
            SingleReader = true,
            SingleWriter = false,
            FullMode = BoundedChannelFullMode.Wait,
            AllowSynchronousContinuations = false
        });
        var healthChannel = Channel.CreateUnbounded<HealthSample>(new UnboundedChannelOptions
        {
            SingleReader = true,
            SingleWriter = true,
            AllowSynchronousContinuations = false
        });

        var requestWriterTask = WriteRequestsAsync(requestChannel.Reader, requestsWriter);
        var healthWriterTask = WriteHealthAsync(healthChannel.Reader, healthWriter);
        var metrics = new RunMetrics(options);
        var selector = new ScenarioSelector(options, fixtures);
        var startedAtUtc = DateTimeOffset.UtcNow;
        var stopwatch = Stopwatch.StartNew();
        using var runCancellation = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        using var client = CreateClient(options);
        using var healthClient = CreateHealthClient(options);
        var normalGate = new RateGate(options.TargetRps);
        var cooldownGate = new RateGate(1);
        var measuredAttackLimit = new MeasuredRequestLimit(options.TotalRequests);
        var stopReason = "completed";

        void RequestStop(string reason)
        {
            reportStopReason(reason);
            Interlocked.CompareExchange(ref stopReason, reason, "completed");
            runCancellation.Cancel();
        }

        var healthTask = ProbeHealthAsync(
            options,
            fixtures,
            metrics,
            healthClient,
            healthChannel.Writer,
            stopwatch,
            runCancellation,
            RequestStop);

        var workers = Enumerable.Range(0, options.Concurrency)
            .Select(worker => RunWorkerAsync(
                worker,
                options,
                fixtures,
                selector,
                metrics,
                client,
                requestChannel.Writer,
                normalGate,
                measuredAttackLimit,
                stopwatch,
                runCancellation.Token))
            .ToArray();

        try
        {
            await Task.WhenAll(workers);
        }
        catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested)
        {
            // A health-triggered or internal stop is a controlled test outcome.
        }
        catch (OperationCanceledException)
        {
            RequestStop("cancelled");
        }

        var measuredLoadElapsedSeconds = Math.Max(
            0,
            Math.Min(stopwatch.Elapsed.TotalSeconds, options.WarmupSeconds + options.DurationSeconds) - options.WarmupSeconds);

        if (!runCancellation.IsCancellationRequested && options.CooldownSeconds > 0)
        {
            await RunCooldownAsync(
                options,
                fixtures,
                selector,
                metrics,
                client,
                cooldownGate,
                requestChannel.Writer,
                stopwatch,
                runCancellation.Token);
        }

        await runCancellation.CancelAsync();
        try
        {
            await healthTask;
        }
        catch (OperationCanceledException)
        {
            // Expected when the coordinated test window closes.
        }

        requestChannel.Writer.TryComplete();
        healthChannel.Writer.TryComplete();
        await Task.WhenAll(requestWriterTask, healthWriterTask);
        stopwatch.Stop();
        metrics.MarkCompleted();

        if (cancellationToken.IsCancellationRequested && stopReason == "completed")
        {
            stopReason = "cancelled";
        }

        var failures = metrics.Evaluate(stopReason);
        var summary = metrics.CreateSummary(
            options,
            stopReason,
            startedAtUtc,
            DateTimeOffset.UtcNow,
            measuredLoadElapsedSeconds,
            failures);
        var json = JsonSerializer.Serialize(summary, new JsonSerializerOptions { WriteIndented = true });
        await WriteTextAsync(summaryPath, json.Replace("\r\n", "\n"));

        return new LoadResult(
            summaryPath,
            requestsPath,
            healthPath,
            metrics.TotalRequests,
            metrics.SuccessRate,
            failures.Count == 0,
            stopReason,
            failures);
    }

    private static StreamWriter OpenWriter(string path)
    {
        var stream = new FileStream(path, FileMode.Create, FileAccess.Write, FileShare.Read, 65536, FileOptions.Asynchronous);
        return new StreamWriter(stream, Utf8NoBom, 65536) { NewLine = "\n" };
    }

    private static async Task WriteTextAsync(string path, string text)
    {
        await using var stream = new FileStream(path, FileMode.Create, FileAccess.Write, FileShare.Read, 65536, FileOptions.Asynchronous);
        await using var writer = new StreamWriter(stream, Utf8NoBom, 65536) { NewLine = "\n" };
        await writer.WriteAsync(text);
    }
    private static async Task RunWorkerAsync(
        int worker,
        Options options,
        FixtureCatalog fixtures,
        ScenarioSelector selector,
        RunMetrics metrics,
        HttpClient client,
        ChannelWriter<RequestSample> writer,
        RateGate rateGate,
        MeasuredRequestLimit measuredLimit,
        Stopwatch stopwatch,
        CancellationToken cancellationToken)
    {
        while (!cancellationToken.IsCancellationRequested)
        {
            var elapsed = stopwatch.Elapsed.TotalSeconds;
            var measuredEnd = options.WarmupSeconds + options.DurationSeconds;
            if (elapsed >= measuredEnd)
            {
                break;
            }

            var phase = elapsed < options.WarmupSeconds ? "warmup" : "measured";
            if (phase == "measured" && !measuredLimit.TryAcquire())
            {
                break;
            }

            await rateGate.WaitAsync(cancellationToken);
            if (stopwatch.Elapsed.TotalSeconds >= measuredEnd)
            {
                break;
            }

            var index = metrics.NextIndex();
            var fixture = selector.Next(index);
            var connectionMode = selector.NextConnectionMode(index);
            var sample = await SendRequestAsync(
                client,
                options.BaseUrl.TrimEnd('/') + "/fixtures/" + fixture.RelativePath,
                fixture,
                phase,
                worker,
                index,
                connectionMode,
                options.RequestTimeoutSeconds,
                cancellationToken);
            metrics.Record(sample);
            await writer.WriteAsync(sample, cancellationToken);
        }
    }

    private static async Task RunCooldownAsync(
        Options options,
        FixtureCatalog fixtures,
        ScenarioSelector selector,
        RunMetrics metrics,
        HttpClient client,
        RateGate rateGate,
        ChannelWriter<RequestSample> writer,
        Stopwatch stopwatch,
        CancellationToken cancellationToken)
    {
        var cooldownEnd = options.WarmupSeconds + options.DurationSeconds + options.CooldownSeconds;
        while (!cancellationToken.IsCancellationRequested && stopwatch.Elapsed.TotalSeconds < cooldownEnd)
        {
            await rateGate.WaitAsync(cancellationToken);
            if (stopwatch.Elapsed.TotalSeconds >= cooldownEnd)
            {
                break;
            }

            var index = metrics.NextIndex();
            var fixture = selector.Next(index);
            var connectionMode = selector.NextConnectionMode(index);
            var sample = await SendRequestAsync(
                client,
                options.BaseUrl.TrimEnd('/') + "/fixtures/" + fixture.RelativePath,
                fixture,
                "cooldown",
                0,
                index,
                connectionMode,
                options.RequestTimeoutSeconds,
                cancellationToken);
            metrics.Record(sample);
            await writer.WriteAsync(sample, cancellationToken);
        }
        _ = fixtures;
    }

    private static async Task ProbeHealthAsync(
        Options options,
        FixtureCatalog fixtures,
        RunMetrics metrics,
        HttpClient client,
        ChannelWriter<HealthSample> writer,
        Stopwatch stopwatch,
        CancellationTokenSource runCancellation,
        Action<string> requestStop)
    {
        Fixture? expected = options.HealthUrl.EndsWith("/fixtures/small-1k.bin", StringComparison.OrdinalIgnoreCase)
            ? fixtures.Small
            : null;
        double? firstFailureAt = null;
        try
        {
            while (!runCancellation.IsCancellationRequested)
            {
                var phase = stopwatch.Elapsed.TotalSeconds < options.WarmupSeconds
                    ? "warmup"
                    : stopwatch.Elapsed.TotalSeconds < options.WarmupSeconds + options.DurationSeconds
                        ? "measured"
                        : "cooldown";
                var started = Stopwatch.GetTimestamp();
                var status = 0;
                var errorType = "";
                var error = "";
                var success = false;
                try
                {
                    using var timeout = CancellationTokenSource.CreateLinkedTokenSource(runCancellation.Token);
                    timeout.CancelAfter(TimeSpan.FromSeconds(options.RequestTimeoutSeconds));
                    using var request = new HttpRequestMessage(HttpMethod.Get, options.HealthUrl);
                    request.Headers.TryAddWithoutValidation("Accept-Encoding", "identity");
                    using var response = await client.SendAsync(request, HttpCompletionOption.ResponseHeadersRead, timeout.Token);
                    status = (int)response.StatusCode;
                    await using var stream = await response.Content.ReadAsStreamAsync(timeout.Token);
                    using var hash = IncrementalHash.CreateHash(HashAlgorithmName.SHA256);
                    var buffer = new byte[81920];
                    long bytes = 0;
                    while (true)
                    {
                        var read = await stream.ReadAsync(buffer, timeout.Token);
                        if (read == 0) break;
                        bytes += read;
                        hash.AppendData(buffer, 0, read);
                    }
                    success = status is >= 200 and < 300;
                    if (!success)
                    {
                        errorType = "status";
                        error = $"Unexpected HTTP status {status}";
                    }
                    else if (expected is not null && bytes != expected.SizeBytes)
                    {
                        success = false;
                        errorType = "size_mismatch";
                        error = $"Expected {expected.SizeBytes} bytes, received {bytes}";
                    }
                    else if (expected is not null && !string.Equals(Convert.ToHexString(hash.GetHashAndReset()), expected.Sha256, StringComparison.OrdinalIgnoreCase))
                    {
                        success = false;
                        errorType = "hash_mismatch";
                        error = "Health fixture SHA-256 mismatch";
                    }
                }
                catch (OperationCanceledException) when (!runCancellation.IsCancellationRequested)
                {
                    errorType = "timeout";
                    error = "Health probe timed out";
                }
                catch (HttpRequestException exception)
                {
                    errorType = "http";
                    error = exception.Message;
                }
                catch (IOException exception)
                {
                    errorType = "io";
                    error = exception.Message;
                }
                catch (Exception exception) when (exception is not OperationCanceledException)
                {
                    errorType = exception.GetType().Name;
                    error = exception.Message;
                }

                var latency = Stopwatch.GetElapsedTime(started).TotalMilliseconds;
                var sample = new HealthSample(
                    DateTimeOffset.UtcNow.ToString("O", CultureInfo.InvariantCulture),
                    phase,
                    stopwatch.Elapsed.TotalSeconds,
                    success,
                    status,
                    latency,
                    errorType,
                    error);
                metrics.RecordHealth(sample);
                await writer.WriteAsync(sample, runCancellation.Token);

                if (success)
                {
                    firstFailureAt = null;
                }
                else
                {
                    firstFailureAt ??= stopwatch.Elapsed.TotalSeconds;
                    if (options.HealthStopSeconds > 0 &&
                        stopwatch.Elapsed.TotalSeconds - firstFailureAt.Value >= options.HealthStopSeconds)
                    {
                        requestStop("health_unavailable");
                        break;
                    }
                }

                await Task.Delay(TimeSpan.FromSeconds(options.HealthIntervalSeconds), runCancellation.Token);
            }
        }
        catch (OperationCanceledException) when (runCancellation.IsCancellationRequested)
        {
            // Cooperative shutdown.
        }
    }
    private static async Task<RequestSample> SendRequestAsync(
        HttpClient client,
        string url,
        Fixture expected,
        string phase,
        int worker,
        long index,
        string connectionMode,
        int timeoutSeconds,
        CancellationToken cancellationToken)
    {
        var timestamp = DateTimeOffset.UtcNow.ToString("O", CultureInfo.InvariantCulture);
        var started = Stopwatch.GetTimestamp();
        var status = 0;
        long actualBytes = 0;
        var actualHash = "";
        var sizeMatches = false;
        var hashMatches = false;
        var errorType = "";
        var error = "";
        try
        {
            using var timeout = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
            timeout.CancelAfter(TimeSpan.FromSeconds(timeoutSeconds));
            using var request = new HttpRequestMessage(HttpMethod.Get, url);
            request.Version = HttpVersion.Version11;
            request.Headers.TryAddWithoutValidation("Accept-Encoding", "identity");
            if (connectionMode == "close")
            {
                request.Headers.ConnectionClose = true;
            }

            using var response = await client.SendAsync(request, HttpCompletionOption.ResponseHeadersRead, timeout.Token);
            status = (int)response.StatusCode;
            await using var stream = await response.Content.ReadAsStreamAsync(timeout.Token);
            using var hash = IncrementalHash.CreateHash(HashAlgorithmName.SHA256);
            var buffer = new byte[81920];
            while (true)
            {
                var read = await stream.ReadAsync(buffer, timeout.Token);
                if (read == 0)
                {
                    break;
                }
                actualBytes += read;
                hash.AppendData(buffer, 0, read);
            }
            actualHash = Convert.ToHexString(hash.GetHashAndReset()).ToLowerInvariant();
            sizeMatches = actualBytes == expected.SizeBytes;
            hashMatches = string.Equals(actualHash, expected.Sha256, StringComparison.OrdinalIgnoreCase);
            if (status != 200)
            {
                errorType = "status";
                error = $"Unexpected HTTP status {status}";
            }
            else if (!sizeMatches)
            {
                errorType = "size_mismatch";
                error = $"Expected {expected.SizeBytes} bytes, received {actualBytes}";
            }
            else if (!hashMatches)
            {
                errorType = "hash_mismatch";
                error = "SHA-256 does not match fixture manifest";
            }
        }
        catch (OperationCanceledException) when (cancellationToken.IsCancellationRequested)
        {
            errorType = "cancelled";
            error = "Request cancelled";
        }
        catch (OperationCanceledException)
        {
            errorType = "timeout";
            error = $"Request exceeded {timeoutSeconds} seconds";
        }
        catch (HttpRequestException exception)
        {
            errorType = "http";
            error = exception.Message;
        }
        catch (IOException exception)
        {
            errorType = "io";
            error = exception.Message;
        }
        catch (Exception exception)
        {
            errorType = exception.GetType().Name;
            error = exception.Message;
        }

        var latency = Stopwatch.GetElapsedTime(started).TotalMilliseconds;
        var success = status == 200 && sizeMatches && hashMatches;
        return new RequestSample(
            timestamp,
            phase,
            worker,
            index,
            expected.RelativePath,
            status,
            latency,
            actualBytes,
            expected.SizeBytes,
            actualHash,
            expected.Sha256,
            sizeMatches,
            hashMatches,
            connectionMode,
            success,
            errorType,
            error);
    }

    private static HttpClient CreateClient(Options options)
    {
        var handler = new SocketsHttpHandler
        {
            UseProxy = false,
            AutomaticDecompression = DecompressionMethods.None,
            ConnectTimeout = TimeSpan.FromSeconds(Math.Min(options.RequestTimeoutSeconds, 30)),
            MaxConnectionsPerServer = options.Concurrency,
            PooledConnectionLifetime = TimeSpan.FromMinutes(15),
            PooledConnectionIdleTimeout = TimeSpan.FromMinutes(2),
            EnableMultipleHttp2Connections = false
        };
        var client = new HttpClient(handler, disposeHandler: true)
        {
            Timeout = Timeout.InfiniteTimeSpan,
            DefaultRequestVersion = HttpVersion.Version11,
            DefaultVersionPolicy = HttpVersionPolicy.RequestVersionOrLower
        };
        client.DefaultRequestHeaders.TryAddWithoutValidation("User-Agent", "EasyProxy.LoadTester/1.0");
        client.DefaultRequestHeaders.ExpectContinue = false;
        return client;
    }

    private static HttpClient CreateHealthClient(Options options)
    {
        var handler = new SocketsHttpHandler
        {
            UseProxy = false,
            AutomaticDecompression = DecompressionMethods.None,
            ConnectTimeout = TimeSpan.FromSeconds(Math.Min(options.RequestTimeoutSeconds, 30)),
            MaxConnectionsPerServer = 1,
            PooledConnectionLifetime = TimeSpan.FromMinutes(5)
        };
        return new HttpClient(handler, disposeHandler: true)
        {
            Timeout = Timeout.InfiniteTimeSpan,
            DefaultRequestVersion = HttpVersion.Version11,
            DefaultVersionPolicy = HttpVersionPolicy.RequestVersionOrLower
        };
    }

    private static async Task WriteRequestsAsync(ChannelReader<RequestSample> reader, StreamWriter writer)
    {
        await foreach (var sample in reader.ReadAllAsync())
        {
            var values = new[]
            {
                sample.TimestampUtc,
                sample.Phase,
                sample.Worker.ToString(CultureInfo.InvariantCulture),
                sample.Index.ToString(CultureInfo.InvariantCulture),
                sample.Path,
                sample.StatusCode.ToString(CultureInfo.InvariantCulture),
                sample.LatencyMs.ToString("F3", CultureInfo.InvariantCulture),
                sample.ActualBytes.ToString(CultureInfo.InvariantCulture),
                sample.ExpectedBytes.ToString(CultureInfo.InvariantCulture),
                sample.ActualSha256,
                sample.ExpectedSha256,
                sample.SizeMatches ? "true" : "false",
                sample.HashMatches ? "true" : "false",
                sample.ConnectionMode,
                sample.Success ? "true" : "false",
                sample.ErrorType,
                sample.Error
            };
            await writer.WriteLineAsync(string.Join(',', values.Select(Csv)));
        }
    }

    private static async Task WriteHealthAsync(ChannelReader<HealthSample> reader, StreamWriter writer)
    {
        await foreach (var sample in reader.ReadAllAsync())
        {
            var values = new[]
            {
                sample.TimestampUtc,
                sample.Phase,
                sample.ElapsedSeconds.ToString("F3", CultureInfo.InvariantCulture),
                sample.Success ? "true" : "false",
                sample.StatusCode.ToString(CultureInfo.InvariantCulture),
                sample.LatencyMs.ToString("F3", CultureInfo.InvariantCulture),
                sample.ErrorType,
                sample.Error
            };
            await writer.WriteLineAsync(string.Join(',', values.Select(Csv)));
        }
    }

    private static string Csv(string value) =>
        value.IndexOfAny(new[] { ',', '"', '\r', '\n' }) >= 0
            ? "\"" + value.Replace("\"", "\"\"") + "\""
            : value;

    private const string RequestCsvHeader =
        "timestamp_utc,phase,worker,index,path,status_code,latency_ms,actual_bytes,expected_bytes,actual_sha256,expected_sha256,size_match,hash_match,connection_mode,success,error_type,error";
    private const string HealthCsvHeader =
        "timestamp_utc,phase,elapsed_seconds,success,status_code,latency_ms,error_type,error";
}
internal sealed class RunMetrics
{
    private readonly Options _options;
    private readonly long[] _latencyHistogram;
    private long _nextIndex;
    private long _total;
    private long _success;
    private long _status5xx;
    private long _hashMismatch;
    private long _sizeMismatch;
    private long _healthTotal;
    private long _healthSuccess;
    private readonly ConcurrentDictionary<string, long> _statusCodes = new(StringComparer.Ordinal);
    private readonly ConcurrentDictionary<string, long> _errors = new(StringComparer.Ordinal);

    public RunMetrics(Options options)
    {
        _options = options;
        _latencyHistogram = new long[Math.Max(1001, options.RequestTimeoutSeconds * 1000 + 2)];
    }

    public long TotalRequests => Interlocked.Read(ref _total);
    public double SuccessRate
    {
        get
        {
            var total = TotalRequests;
            return total == 0 ? 0 : Interlocked.Read(ref _success) / (double)total;
        }
    }

    public long NextIndex() => Interlocked.Increment(ref _nextIndex);

    public void Record(RequestSample sample)
    {
        if (!sample.Phase.Equals("measured", StringComparison.Ordinal))
        {
            return;
        }

        Interlocked.Increment(ref _total);
        if (sample.Success)
        {
            Interlocked.Increment(ref _success);
        }
        if (sample.StatusCode is >= 500 and <= 599)
        {
            Interlocked.Increment(ref _status5xx);
        }
        if (!sample.HashMatches) Interlocked.Increment(ref _hashMismatch);
        if (!sample.SizeMatches) Interlocked.Increment(ref _sizeMismatch);
        _statusCodes.AddOrUpdate(sample.StatusCode.ToString(CultureInfo.InvariantCulture), 1, static (_, count) => count + 1);
        if (!sample.Success)
        {
            var key = string.IsNullOrWhiteSpace(sample.ErrorType) ? "unknown" : sample.ErrorType;
            _errors.AddOrUpdate(key, 1, static (_, count) => count + 1);
        }

        var bucket = Math.Clamp((int)Math.Ceiling(sample.LatencyMs), 0, _latencyHistogram.Length - 1);
        Interlocked.Increment(ref _latencyHistogram[bucket]);
    }

    public void RecordHealth(HealthSample sample)
    {
        Interlocked.Increment(ref _healthTotal);
        if (sample.Success)
        {
            Interlocked.Increment(ref _healthSuccess);
        }
    }

    public double HealthAvailability
    {
        get
        {
            var total = Interlocked.Read(ref _healthTotal);
            return total == 0 ? 0 : Interlocked.Read(ref _healthSuccess) / (double)total;
        }
    }

    public void MarkCompleted()
    {
    }

    public IReadOnlyList<string> Evaluate(string stopReason)
    {
        var failures = new List<string>();
        var total = TotalRequests;
        if (total == 0)
        {
            failures.Add("no measured requests completed");
        }
        if (_options.MinSuccessRate is double minSuccessRate && SuccessRate < minSuccessRate)
        {
            failures.Add($"success rate {SuccessRate:F6} < {minSuccessRate:F6}");
        }
        var p95 = Percentile(0.95);
        var p99 = Percentile(0.99);
        if (_options.MaxP95Ms is double maxP95 && p95 > maxP95)
        {
            failures.Add($"p95 {p95:F3} ms > {maxP95:F3} ms");
        }
        if (_options.MaxP99Ms is double maxP99 && p99 > maxP99)
        {
            failures.Add($"p99 {p99:F3} ms > {maxP99:F3} ms");
        }
        var status5xx = Interlocked.Read(ref _status5xx);
        if (_options.FailOn5xx && status5xx > _options.Expected5xx)
        {
            failures.Add($"unexpected 5xx {status5xx} > allowed {_options.Expected5xx}");
        }
        var hashMismatch = Interlocked.Read(ref _hashMismatch);
        if (hashMismatch > 0)
        {
            failures.Add($"hash mismatches: {hashMismatch}");
        }
        var sizeMismatch = Interlocked.Read(ref _sizeMismatch);
        if (sizeMismatch > 0)
        {
            failures.Add($"size mismatches: {sizeMismatch}");
        }
        if (_options.MinHealthRate is double minHealthRate && HealthAvailability < minHealthRate)
        {
            failures.Add($"health availability {HealthAvailability:F6} < {minHealthRate:F6}");
        }
        if (stopReason == "health_unavailable")
        {
            failures.Add("health probe triggered a safety stop");
        }
        if (stopReason == "cancelled")
        {
            failures.Add("run cancelled before completion");
        }
        return failures;
    }

    public object CreateSummary(
        Options options,
        string stopReason,
        DateTimeOffset startedAtUtc,
        DateTimeOffset completedAtUtc,
        double measuredElapsedSeconds,
        IReadOnlyList<string> failures)
    {
        var total = TotalRequests;
        return new
        {
            schemaVersion = 1,
            tool = "EasyProxy.LoadTester",
            startedAtUtc,
            completedAtUtc,
            stopReason,
            passed = failures.Count == 0,
            options = new
            {
                options.BaseUrl,
                options.Scenario,
                options.Concurrency,
                options.WarmupSeconds,
                options.DurationSeconds,
                options.CooldownSeconds,
                options.TargetRps,
                options.TotalRequests,
                options.Connections,
                options.RequestTimeoutSeconds,
                options.HealthUrl,
                options.HealthIntervalSeconds,
                options.HealthStopSeconds,
                options.MinSuccessRate,
                options.MaxP95Ms,
                options.MaxP99Ms,
                options.MinHealthRate,
                options.FailOn5xx,
                options.Expected5xx,
                options.RandomSeed
            },
            measured = new
            {
                elapsedSeconds = Math.Round(measuredElapsedSeconds, 3),
                totalRequests = total,
                successfulRequests = Interlocked.Read(ref _success),
                successRate = Math.Round(SuccessRate, 6),
                requestsPerSecond = Math.Round(total / Math.Max(measuredElapsedSeconds, 0.001), 3),
                latencyMs = new
                {
                    p50 = Math.Round(Percentile(0.50), 3),
                    p95 = Math.Round(Percentile(0.95), 3),
                    p99 = Math.Round(Percentile(0.99), 3),
                    max = Math.Round(Percentile(1.0), 3)
                },
                statusCodes = _statusCodes.OrderBy(static item => item.Key, StringComparer.Ordinal).ToDictionary(static item => item.Key, static item => item.Value),
                errors = _errors.OrderBy(static item => item.Key, StringComparer.Ordinal).ToDictionary(static item => item.Key, static item => item.Value),
                server5xx = Interlocked.Read(ref _status5xx),
                hashMismatches = Interlocked.Read(ref _hashMismatch),
                sizeMismatches = Interlocked.Read(ref _sizeMismatch)
            },
            health = new
            {
                probes = Interlocked.Read(ref _healthTotal),
                successfulProbes = Interlocked.Read(ref _healthSuccess),
                availability = Math.Round(HealthAvailability, 6)
            },
            thresholdFailures = failures
        };
    }

    private double Percentile(double percentile)
    {
        var total = TotalRequests;
        if (total == 0) return 0;
        var target = Math.Max(1L, (long)Math.Ceiling(total * percentile));
        var cumulative = 0L;
        for (var index = 0; index < _latencyHistogram.Length; index++)
        {
            cumulative += Interlocked.Read(ref _latencyHistogram[index]);
            if (cumulative >= target)
            {
                return index;
            }
        }
        return _latencyHistogram.Length - 1;
    }
}

internal sealed class ScenarioSelector
{
    private readonly Options _options;
    private readonly FixtureCatalog _fixtures;

    public ScenarioSelector(Options options, FixtureCatalog fixtures)
    {
        _options = options;
        _fixtures = fixtures;
    }

    public Fixture Next(long index)
    {
        return _options.Scenario switch
        {
            "small" => _fixtures.Small,
            "distinct" => _fixtures.Distinct[(int)((index - 1) % _fixtures.Distinct.Count)],
            "large" => _fixtures.Large,
            "slow" => _fixtures.Slow,
            "mixed" => NextMixed(index),
            "connection" => _fixtures.Small,
            _ => throw new InvalidOperationException($"Unsupported scenario: {_options.Scenario}")
        };
    }

    public string NextConnectionMode(long index)
    {
        if (_options.Connections != "mixed")
        {
            return _options.Connections;
        }
        return (Mix64((ulong)index ^ (ulong)(uint)_options.RandomSeed) & 1) == 0 ? "keep-alive" : "close";
    }

    private Fixture NextMixed(long index)
    {
        var bucket = Mix64((ulong)index * 0x9E3779B97F4A7C15UL ^ (ulong)(uint)_options.RandomSeed) % 100;
        return bucket < 70 ? _fixtures.Small : bucket < 90 ? _fixtures.Large : _fixtures.Slow;
    }

    private static ulong Mix64(ulong value)
    {
        value ^= value >> 30;
        value *= 0xBF58476D1CE4E5B9UL;
        value ^= value >> 27;
        value *= 0x94D049BB133111EBUL;
        return value ^ (value >> 31);
    }
}

internal sealed class RateGate
{
    private readonly int _requestsPerSecond;
    private readonly object _sync = new();
    private long _nextTimestamp;

    public RateGate(int requestsPerSecond)
    {
        _requestsPerSecond = requestsPerSecond;
    }

    public async ValueTask WaitAsync(CancellationToken cancellationToken)
    {
        if (_requestsPerSecond <= 0) return;
        long target;
        var now = Stopwatch.GetTimestamp();
        lock (_sync)
        {
            target = Math.Max(now, _nextTimestamp);
            _nextTimestamp = target + Math.Max(1, Stopwatch.Frequency / _requestsPerSecond);
        }
        var delaySeconds = (target - now) / (double)Stopwatch.Frequency;
        if (delaySeconds > 0)
        {
            await Task.Delay(TimeSpan.FromSeconds(delaySeconds), cancellationToken);
        }
    }
}

internal sealed class MeasuredRequestLimit
{
    private readonly long _limit;
    private long _acquired;

    public MeasuredRequestLimit(long limit)
    {
        _limit = limit;
    }

    public bool TryAcquire()
    {
        return _limit <= 0 || Interlocked.Increment(ref _acquired) <= _limit;
    }
}