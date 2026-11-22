package com.zhikao.media.storage.local;

import com.zhikao.common.exceptions.CommonException;
import com.zhikao.media.storage.IFileStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * 本地磁盘文件存储。
 * <p>
 * 个人开发者没有 OSS/COS 账号时，把 {@code zk.platform.file} 设为 {@code LOCAL} 就用这个实现：
 * 文件落到本地目录，数据库只存 key，{@code GET /medias/{id}} 时再从磁盘读回来。
 * 接口与阿里云/腾讯云实现完全一致，切换不需要改业务代码。
 * </p>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "zk.platform", name = "file", havingValue = "LOCAL")
public class LocalFileStorage implements IFileStorage {

    private final Path root;

    public LocalFileStorage(@Value("${zk.local.upload-dir:./uploads}") String dir) throws IOException {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(root);
        log.info("本地文件存储已启用，目录：{}", root);
    }

    @Override
    public String uploadFile(String key, InputStream inputStream, long contentLength) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("[本地存储] 已保存 {}（{} 字节）", key, contentLength);
            return "local:" + key;
        } catch (IOException e) {
            throw new CommonException("本地文件写入失败：" + key, e);
        }
    }

    @Override
    public InputStream downloadFile(String key) {
        Path target = resolve(key);
        if (!Files.exists(target)) {
            throw new CommonException("文件不存在：" + key);
        }
        try {
            return Files.newInputStream(target);
        } catch (IOException e) {
            throw new CommonException("本地文件读取失败：" + key, e);
        }
    }

    @Override
    public void deleteFile(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            log.warn("[本地存储] 删除失败 {}", key, e);
        }
    }

    @Override
    public void deleteFiles(List<String> keys) {
        if (keys != null) {
            keys.forEach(this::deleteFile);
        }
    }

    /**
     * 防目录穿越：任何 key 都必须落在 root 之下
     */
    private Path resolve(String key) {
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) {
            throw new CommonException("非法的文件 key：" + key);
        }
        return p;
    }
}
