package com.fruitrec.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fruitrec.common.BusinessException;
import com.fruitrec.config.FruitProperties;
import com.fruitrec.dto.PredictResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ★ 方案 D 的核心：Java 用 ProcessBuilder 直接调 python 脚本，读它输出的 JSON。
 *
 * 为什么不用 HTTP 常驻服务：零新服务、零端口，演示时只起一个进程。
 * 代价：每次调用都要重新加载模型，约 5-8 秒。
 * 以后想优化 → 把脚本换成常驻 FastAPI，这个类只改 execute() 一个方法。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PythonService {

    private final FruitProperties props;

    /** 下划线命名策略，用来把 cost_ms / class_index / name_cn 映射成驼峰 */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * 调用 Python 识别一张图。
     *
     * @param imagePath 图片绝对路径
     * @return 解析后的识别结果；失败时抛 BusinessException
     */
    public PredictResult predict(String imagePath) {
        List<String> cmd = new ArrayList<>();
        cmd.add(props.getPython().getExe());
        cmd.add(props.getPython().getScript());
        cmd.add(imagePath);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        // Windows 下强制 Python 用 UTF-8 输出，否则中文类别名会乱码
        pb.environment().put("PYTHONIOENCODING", "utf-8");
        pb.redirectErrorStream(false);

        log.info("调用 Python: {}", String.join(" ", cmd));
        long t0 = System.currentTimeMillis();

        try {
            Process process = pb.start();

            // ⚠️ 必须同时排空 stdout 和 stderr。
            //    只读一个、另一个写满缓冲区会死锁（torch 的警告会往 stderr 写）。
            StringBuilder errBuf = new StringBuilder();
            Thread errDrainer = new Thread(() -> {
                try (InputStream err = process.getErrorStream()) {
                    errBuf.append(new String(err.readAllBytes(), StandardCharsets.UTF_8));
                } catch (IOException ignored) {
                }
            });
            errDrainer.setDaemon(true);
            errDrainer.start();

            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            if (!process.waitFor(props.getPython().getTimeoutSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new BusinessException(500, "识别超时（超过 " + props.getPython().getTimeoutSeconds() + " 秒）");
            }
            errDrainer.join(2000);

            if (process.exitValue() != 0) {
                log.error("Python 退出码 {}，stderr:\n{}", process.exitValue(), errBuf);
                throw new BusinessException(500, "识别失败，Python 进程异常退出");
            }

            String json = stdout.trim();
            if (json.isEmpty()) {
                log.error("Python 没有输出，stderr:\n{}", errBuf);
                throw new BusinessException(500, "识别失败，Python 没有返回结果");
            }

            PredictResult result = MAPPER.readValue(json, PredictResult.class);
            if (!Boolean.TRUE.equals(result.getSuccess())) {
                throw new BusinessException(500, "识别失败：" + result.getError());
            }

            log.info("识别完成，耗时 {} ms（Python 自报 {} ms）",
                    System.currentTimeMillis() - t0, result.getCostMs());
            return result;

        } catch (IOException e) {
            log.error("无法启动 Python 进程，检查 fruit.python.exe / script 配置", e);
            throw new BusinessException(500, "无法调用识别程序，请检查 Python 配置");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(500, "识别被中断");
        }
    }
}
