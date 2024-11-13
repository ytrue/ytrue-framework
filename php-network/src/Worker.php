<?php

namespace Network;

use RuntimeException;
use stdClass;
use Stringable;
use Throwable;

class Worker
{
    /**
     * 启动文件。
     *
     * @var string 存储程序启动时的文件路径。
     */
    protected static string $startFile;


    /**
     * 标准输出流。  fopen('php://stdout', 'w');
     *
     * @var resource 输出流资源，用于输出信息。
     */
    public static $outputStream;


    /**
     * 判断输出流是否支持装饰格式。
     *
     * @var bool 如果支持装饰格式，值为 true；否则为 false。
     */
    protected static bool $outputDecorated;


    /**
     * 守护进程模式运行.
     *
     * @var bool
     */
    public static bool $daemonize = false;

    /**
     * 日志文件路径
     *
     * @var string
     */
    public static string $logFile;


    /**
     * worker进程的数量。
     *
     * @var int 进程数量，默认为1。
     */
    public int $count = 1;


    /**
     * Socket的上下文。
     *
     * @var resource|null Socket的上下文资源，默认为null。
     */
    protected $socketContext = null;

    /**
     * Socket名称。格式类似于：http://0.0.0.0:80 。
     *
     * @var string Socket名称，默认为空字符串。
     */
    protected string $socketName = '';

    /**
     * 默认的backlog。backlog是指等待连接队列的最大长度。
     *
     * @var int 默认的backlog值为102400。
     */
    public const int DEFAULT_BACKLOG = 102400;

    /**
     * 上下文对象。
     *
     * @var stdClass 上下文对象，保存与worker相关的状态信息。
     */
    protected stdClass $context;

    /**
     * 所有worker进程的PID映射。
     * 格式为：[worker_id => [pid => pid, pid => pid, ...], ...]
     *
     * @var array 保存worker实例的进程ID映射数组。
     */
    protected static array $pidMap = [];

    /**
     * 所有worker实例。
     *
     * @var Worker[] [workerId => Worker实例] 保存所有worker实例的数组。
     */
    protected static array $workers = [];

    /**
     * Worker对象的哈希ID（唯一标识符）。
     *
     * @var ?string Worker对象的唯一标识符，默认为null。
     */
    protected ?string $workerId = null;


    /**
     * PHP 内置错误类型。
     *
     * @var array[int,string] 错误类型的数组，键为错误代码，值为错误名称。
     */
    public const array ERROR_TYPE = [
        E_ERROR => 'E_ERROR',                     // 1 - 运行时错误
        E_WARNING => 'E_WARNING',                 // 2 - 运行时警告（非致命）
        E_PARSE => 'E_PARSE',                     // 4 - 解析错误
        E_NOTICE => 'E_NOTICE',                   // 8 - 通知
        E_CORE_ERROR => 'E_CORE_ERROR',           // 16 - PHP 内部错误
        E_CORE_WARNING => 'E_CORE_WARNING',       // 32 - PHP 内部警告
        E_COMPILE_ERROR => 'E_COMPILE_ERROR',     // 64 - 编译错误
        E_COMPILE_WARNING => 'E_COMPILE_WARNING', // 128 - 编译警告
        E_USER_ERROR => 'E_USER_ERROR',           // 256 - 用户产生的错误
        E_USER_WARNING => 'E_USER_WARNING',       // 512 - 用户产生的警告
        E_USER_NOTICE => 'E_USER_NOTICE',         // 1024 - 用户产生的通知
        E_STRICT => 'E_STRICT',                   // 2048 - 严格标准
        E_RECOVERABLE_ERROR => 'E_RECOVERABLE_ERROR', // 4096 - 可恢复的致命错误
        E_DEPRECATED => 'E_DEPRECATED',           // 8192 - 已废弃
        E_USER_DEPRECATED => 'E_USER_DEPRECATED'  // 16384 - 用户产生的废弃通知
    ];


    /**
     * 构造函数。
     *
     * @param string|null $socketName Socket名称，可选参数。
     * @param array $socketContext Socket上下文选项数组，可选参数。
     */
    public function __construct(string $socketName = null, array $socketContext = [])
    {
        // 保存所有的worker实例。
        // 为当前worker生成唯一的ID。
        $this->workerId = spl_object_hash($this);
        // 初始化上下文为一个空的stdClass对象。
        $this->context = new stdClass();
        // 将当前worker实例保存到workers数组中。
        static::$workers[$this->workerId] = $this;
        // 初始化进程ID映射数组。
        static::$pidMap[$this->workerId] = [];

        // Socket的上下文配置。
        // 如果传入了socket名称。
        if ($socketName) {
            // 设置socket名称。
            $this->socketName = $socketName;
            // 设置socket上下文的backlog参数，默认为DEFAULT_BACKLOG。
            $socketContext['socket']['backlog'] ??= static::DEFAULT_BACKLOG;

            // 使用给定的上下文选项创建socket上下文。
            // stream_context_create 的主要作用是允许你为流操作自定义配置，并将这些配置应用于特定的流资源。它提供了对流行为的细粒度控制
            // resource stream_context_create ([ array $options = [] [, array $params = [] ]] )
            // $options: 一个关联数组，用于定义流上下文选项。每个键代表流传输协议（如 http, https, ftp, socket），每个键的值是一个关联数组，用于指定该协议下的特定选项。
            //  $params: 一个可选的关联数组，用于设置其他的上下文参数（如 notification 函数）
            $this->socketContext = stream_context_create($socketContext);
        }
    }


    /**
     * 运行所有的 worker 实例。
     *
     * @return void 没有返回值。
     */
    public static function runAll(): void
    {
        try {

            // 检查 PHP 的运行环境（如是否为 CLI 模式）。
            static::checkSapiEnv();

            // 初始化标准输出（stdout），可能用于日志或输出重定向。
            self::initStdOut();

            // 初始化环境或类的相关设置。
            static::init();
//
//            // 解析命令行参数（如启动、停止、重启等命令）。
//            static::parseCommand();
//
//            // 加锁，防止多个进程同时操作某些资源。
//            static::lock();
//
//            // 将进程转为守护进程模式（后台运行）。
//            static::daemonize();
//
//            // 初始化所有的 worker 实例。
//            static::initWorkers();
//
//            // 安装信号处理器，用于捕捉和处理系统信号（如终止信号）。
//            static::installSignal();
//
//            // 保存主进程的 PID（进程ID），用于管理。
//            static::saveMasterPid();
//
//            // 解锁资源。
//            static::lock(LOCK_UN);
//
//            // 显示用户界面（UI），通常是一些运行信息。
//            static::displayUI();
//
//            // 分叉（fork）worker进程，开始工作。
//            static::forkWorkers();
//
//            // 重置标准输出（stdout）。
//            static::resetStd();
//
//            // 监控 worker 进程的状态，处理异常或自动重启等。
//            static::monitorWorkers();
        } catch (Throwable $e) {
            // 捕获异常并记录错误日志。
            static::log($e);
        }
    }

    /**
     * 初始化方法。
     *
     * @return void 没有返回值。
     */
    protected static function init(): void
    {
        // 设置全局错误处理函数。
        set_error_handler(static function (int $code, string $msg, string $file, int $line): bool {
            // 使用安全输出函数输出错误信息。
            static::safeEcho(sprintf("%s \"%s\" in file %s on line %d\n", static::getErrorType($code), $msg, $file, $line));
            return true; // 返回 true 表示错误已经被处理。
        });

        // 获取调用栈信息，记录启动文件。
        $backtrace = debug_backtrace(DEBUG_BACKTRACE_IGNORE_ARGS);
        static::$startFile ??= end($backtrace)['file']; // 记录当前启动文件。  Worker::runAll 启动得类文件
        $startFilePrefix = hash('xxh64', static::$startFile); // 生成启动文件的哈希前缀。



//        // 设置 PID 文件。
//        static::$pidFile ??= sprintf('%s/workerman.%s.pid', dirname(__DIR__), $startFilePrefix);
//
//        // 设置状态文件。
//        static::$statusFile ??= sprintf('%s/workerman.%s.status', dirname(__DIR__), $startFilePrefix);
//        static::$statisticsFile ??= static::$statusFile; // 统计文件使用状态文件。
//        static::$connectionsFile ??= static::$statusFile . '.connection'; // 连接文件。
//
//        // 设置日志文件。
//        static::$logFile ??= sprintf('%s/workerman.log', dirname(__DIR__, 2));
//
//        // 如果日志文件不存在且不是 /dev/null，则创建日志文件。
//        if (!is_file(static::$logFile) && static::$logFile !== '/dev/null') {
//            // 如果日志目录不存在，尝试创建目录。
//            if (!is_dir(dirname(static::$logFile))) {
//                @mkdir(dirname(static::$logFile), 0777, true); // 创建目录并设置权限。
//            }
//            touch(static::$logFile); // 创建日志文件。
//            chmod(static::$logFile, 0644); // 设置文件权限。
//        }
//
//        // 初始化状态。
//        static::$status = static::STATUS_STARTING; // 设置状态为启动中。
//
//        // 初始化全局事件。
//        static::initGlobalEvent();
//
//        // 记录全局统计信息的启动时间戳。
//        static::$globalStatistics['start_timestamp'] = time();
//
//        // 设置进程标题。
//        static::setProcessTitle('WorkerMan: master process  start_file=' . static::$startFile);
//
//        // 初始化 Worker ID 数据。
//        static::initId();

        // 初始化定时器。
        // Timer::init();
    }

    /**
     * 根据错误代码获取错误信息。
     *
     * @param int $type 错误类型的代码。
     * @return string 返回对应的错误信息，如果没有找到则返回空字符串。
     */
    protected static function getErrorType(int $type): string
    {
        return self::ERROR_TYPE[$type] ?? ''; // 从错误类型数组中获取对应的错误信息。
    }


    /**
     * 检查 SAPI（服务器 API）。
     *
     * @return void 没有返回值。
     */
    protected static function checkSapiEnv(): void
    {
        // 仅在 CLI（命令行接口）和微型服务器环境下运行。
        if (!in_array(PHP_SAPI, ['cli', 'micro'])) {
            exit("只能在命令行模式下运行\n");
        }
    }

    private static function initStdOut(): void
    {
        // 定义一个默认流的获取函数，如果 STDOUT 常量已定义，则返回其值；
        // 否则尝试打开 php://stdout 流，如果失败则打开 php://output 流。
        $defaultStream = fn() => defined('STDOUT') ? STDOUT : (@fopen('php://stdout', 'w') ?: fopen('php://output', 'w'));

        // 如果静态属性 $outputStream 还没有初始化，则调用默认流获取函数进行初始化。
        static::$outputStream ??= $defaultStream(); //@phpstan-ignore-line

        // 检查 $outputStream 是否为有效的流资源，如果不是，抛出异常。
        if (!is_resource(self::$outputStream) || get_resource_type(self::$outputStream) !== 'stream') {
            $type = get_debug_type(self::$outputStream);
            // 重新初始化 $outputStream。
            static::$outputStream = $defaultStream();
            // 抛出运行时异常，提示 $outputStream 必须是一个流资源，给出当前类型。
            throw new RuntimeException(sprintf('The $outputStream must to be a stream, %s given', $type));
        }

        // 如果静态属性 $outputDecorated 还没有初始化，检查是否支持颜色输出。
        static::$outputDecorated ??= self::hasColorSupport();
    }

    /**
     * 判断是否支持颜色输出（从 Symfony Console 借用）。
     *
     * @link https://github.com/symfony/console/blob/0d14a9f6d04d4ac38a8cea1171f4554e325dae92/Output/StreamOutput.php#L92
     *
     * @return bool 如果支持颜色输出，则返回 true；否则返回 false。
     */
    private static function hasColorSupport(): bool
    {
        // 根据 https://no-color.org/ 进行判断
        // 如果环境变量 NO_COLOR 被设置，则不支持颜色输出。
        if (getenv('NO_COLOR') !== false) {
            return false;
        }

        // 如果 TERM_PROGRAM 环境变量为 'Hyper'，则支持颜色输出。
        if (getenv('TERM_PROGRAM') === 'Hyper') {
            return true;
        }

        // Windows 系统下的处理
        if (DIRECTORY_SEPARATOR === '\\') {
            // 检查 Windows 环境下是否支持 VT100 码，或 ANSICON 环境变量是否存在，
            // 或 ConEmuANSI 环境变量是否为 'ON'，或者 TERM 环境变量是否为 'xterm'。
            return (function_exists('sapi_windows_vt100_support') && @sapi_windows_vt100_support(self::$outputStream))
                || getenv('ANSICON') !== false
                || getenv('ConEmuANSI') === 'ON'
                || getenv('TERM') === 'xterm';
        }

        // 在非 Windows 系统下，检查输出流是否为终端。
        return stream_isatty(self::$outputStream);
    }


    /**
     * 记录日志。
     *
     * @param Stringable|string $msg 日志信息，可以是可转换为字符串的对象或字符串。
     * @param bool $decorated 是否装饰日志输出，例如是否带有颜色或其他格式。默认值为 false。
     * @return void 没有返回值。
     */
    public static function log(Stringable|string $msg, bool $decorated = false): void
    {
        // 将日志信息去除首尾空格并转换为字符串。
        $msg = trim((string)$msg);

        // 如果程序没有以守护进程模式运行，输出日志信息到控制台。
        if (!static::$daemonize) {
            static::safeEcho("$msg\n", $decorated); // 安全地输出日志信息，是否装饰取决于$decorated。
        }

        // 如果设置了日志文件路径，将日志写入文件。
        if (isset(static::$logFile)) {
            // 获取进程ID，如果是Unix系统，使用posix_getpid()获取，否则默认为1。
            $pid = DIRECTORY_SEPARATOR === '/' ? posix_getpid() : 1;

            // 将日志内容格式化为 "时间 pid:进程ID 日志信息" 的形式，并附加到日志文件中，使用文件锁定避免并发写入问题。
            file_put_contents(static::$logFile, sprintf("%s pid:%d %s\n", date('Y-m-d H:i:s'), $pid, $msg), FILE_APPEND | LOCK_EX);
        }
    }


    /**
     * 安全输出（Safe Echo）。
     *
     * @param string $msg 输出的信息。
     * @param bool $decorated 是否装饰输出内容，默认为 false。
     * @return void 没有返回值。
     */
    public static function safeEcho(string $msg, bool $decorated = false): void
    {
        // 如果设置了装饰输出并且 $decorated 为 true，则设置输出的格式。
        if ((static::$outputDecorated ?? false) && $decorated) {
            // 设置装饰格式：返回上一行、清除当前行、设置颜色。
            $line = "\033[1A\n\033[K"; // 回到上一行并清空。
            $white = "\033[47;30m";    // 白底黑字。
            $green = "\033[32;40m";    // 绿字黑底。
            $end = "\033[0m";          // 结束样式。
        } else {
            // 如果不需要装饰，则设置为空字符串。
            $line = '';
            $white = '';
            $green = '';
            $end = '';
        }

        // 替换消息中的自定义标记 `<n>`, `<w>`, `<g>` 为相应的 ANSI 格式化代码。
        $msg = str_replace(['<n>', '<w>', '<g>'], [$line, $white, $green], $msg);
        $msg = str_replace(['</n>', '</w>', '</g>'], $end, $msg);

        // 设置错误处理器，防止输出错误中断执行。
        set_error_handler(static fn(): bool => true);

        // 如果输出流未结束，则将消息写入输出流。
        if (!feof(self::$outputStream)) {
            fwrite(self::$outputStream, $msg);  // 将信息写入输出流。
            fflush(self::$outputStream);        // 刷新输出缓冲区，立即输出内容。
        }
        // 恢复默认错误处理器。
        restore_error_handler();
    }
}
