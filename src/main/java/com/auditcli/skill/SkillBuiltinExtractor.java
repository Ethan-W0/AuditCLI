package com.auditcli.skill;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * 把 jar 内 resources/skills/&lt;name&gt;/ 解压到 ~/.auditcli/skills-cache/&lt;name&gt;/。
 *
 * 解压策略：通过 .version 文件标记当前 jar 内置版本。版本一致跳过；不一致或缺失则覆盖整个目录。
 *
 * 小型内置 skill 文件清单可硬编码；大型 skill 可用 manifest.txt 列出资源，
 * 避免 jar 内 resource walk 的跨平台问题。
 */
public final class SkillBuiltinExtractor {

    /** 当 web-access 内容有破坏性更新时上调，触发缓存重建。 */
    public static final String CURRENT_VERSION = "1.0.0";

    private static final List<BuiltinSkillSpec> BUILTIN_SKILLS = List.of(
            new BuiltinSkillSpec("web-access", List.of(
                    "SKILL.md",
                    "references/cdp-cheatsheet.md",
                    "references/site-patterns/github.com.md",
                    "references/site-patterns/juejin.cn.md",
                    "references/site-patterns/mp.weixin.qq.com.md",
                    "references/site-patterns/x.com.md",
                    "references/site-patterns/xiaohongshu.com.md",
                    "references/site-patterns/zhuanlan.zhihu.com.md"
            )),
            new BuiltinSkillSpec("audit-code", manifestFiles("audit-code"))
    );

    private final Path cacheRoot;

    public SkillBuiltinExtractor(Path cacheRoot) {
        this.cacheRoot = cacheRoot;
    }

    public Path cacheRoot() {
        return cacheRoot;
    }

    public List<String> builtinSkillNames() {
        return BUILTIN_SKILLS.stream().map(BuiltinSkillSpec::name).toList();
    }

    public Path skillCacheDir(String skillName) {
        return cacheRoot.resolve(skillName);
    }

    public void extractAll() throws IOException {
        Files.createDirectories(cacheRoot);
        for (BuiltinSkillSpec spec : BUILTIN_SKILLS) {
            extract(spec);
        }
    }

    private void extract(BuiltinSkillSpec spec) throws IOException {
        Path skillDir = cacheRoot.resolve(spec.name());
        Path versionFile = skillDir.resolve(".version");
        if (Files.exists(versionFile)) {
            String existing = Files.readString(versionFile).trim();
            if (CURRENT_VERSION.equals(existing)) {
                return;
            }
        }
        if (Files.exists(skillDir)) {
            deleteRecursive(skillDir);
        }
        Files.createDirectories(skillDir);
        for (String relative : spec.files()) {
            String resourcePath = "skills/" + spec.name() + "/" + relative;
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                if (in == null) {
                    System.err.println("⚠️ 内置 skill 资源缺失: " + resourcePath);
                    continue;
                }
                Path target = skillDir.resolve(relative);
                Files.createDirectories(target.getParent());
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        Files.writeString(versionFile, CURRENT_VERSION);
    }

    private static void deleteRecursive(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (var stream = Files.walk(dir)) {
            stream.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    private static List<String> manifestFiles(String skillName) {
        String resourcePath = "skills/" + skillName + "/manifest.txt";
        try (InputStream in = SkillBuiltinExtractor.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                System.err.println("⚠️ 内置 skill manifest 缺失: " + resourcePath);
                return List.of("SKILL.md");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .toList();
            }
        } catch (IOException e) {
            System.err.println("⚠️ 读取内置 skill manifest 失败: " + resourcePath + ": " + e.getMessage());
            return List.of("SKILL.md");
        }
    }

    private record BuiltinSkillSpec(String name, List<String> files) {
    }
}
