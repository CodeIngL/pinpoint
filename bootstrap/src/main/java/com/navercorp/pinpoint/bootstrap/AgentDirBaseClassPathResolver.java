/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.bootstrap;



import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 路径解析器，维护了agent的各种依赖赖，以及插件包。
 * @author emeroad
 */
public class AgentDirBaseClassPathResolver implements ClassPathResolver {

    private final BootLogger logger = BootLogger.getLogger(this.getClass().getName());

    static final String VERSION_PATTERN = "(-[0-9]+\\.[0-9]+\\.[0-9]+((\\-SNAPSHOT)|(-RC[0-9]+))?)?";
    static final Pattern DEFAULT_AGENT_PATTERN = compile("pinpoint-bootstrap" + VERSION_PATTERN + "\\.jar");
    static final Pattern DEFAULT_AGENT_COMMONS_PATTERN = compile("pinpoint-commons" + VERSION_PATTERN + "\\.jar");
    static final Pattern DEFAULT_AGENT_CORE_PATTERN = compile("pinpoint-bootstrap-core" + VERSION_PATTERN + "\\.jar");
    static final Pattern DEFAULT_AGENT_CORE_OPTIONAL_PATTERN = compile("pinpoint-bootstrap-core-optional" + VERSION_PATTERN + "\\.jar");
    static final Pattern DEFAULT_ANNOTATIONS = compile("pinpoint-annotations" + VERSION_PATTERN + "\\.jar");

    //pinpoint-bootstrap-xxx.jar
    private final Pattern agentPattern;
    //pinpoint-commons-xxx.jar
    private final Pattern agentCommonsPattern;
    //pinpoint-bootstrap-core-xxx.jar
    private final Pattern agentCorePattern;
    //pinpoint-bootstrap-core-optional-xxx.jar
    private final Pattern agentCoreOptionalPattern;
    //pinpoint-annotations-xxx.jar
    private final Pattern annotationsPattern;

    //java.class.path的值
    private String classPath;

    //agent名字-->--javaagent:xx.jar的xx.jar
    private String agentJarName;
    //agent路径在classpath中呈现的路径
    private String agentJarFullPath;
    //agent所在目录，绝对路径
    private String agentDirPath;

    //配置文件后缀.
    private List<String> fileExtensionList;
    //agent所在目录，绝对路径，boot子目录下
    private String pinpointCommonsJar;
    //agent所在目录，绝对路径，boot子目录下
    private String bootStrapCoreJar;
    //agent所在目录，绝对路径，boot子目录下
    private String bootStrapCoreOptionalJar;
    //agent所在目录，绝对路径，boot子目录下
    private String annotationsJar;

    private BootstrapJarFile bootstrapJarFile;

    private static Pattern compile(String regex) {
        return Pattern.compile(regex);
    }

    /**
     * 使用系统路径java.class.path的路径来构建类路径解析器
     */
    public AgentDirBaseClassPathResolver() {
        this(getClassPathFromSystemProperty());
    }


    /**
     * @see AgentDirBaseClassPathResolver
     * @param classPath 指定的路径
     */
    public AgentDirBaseClassPathResolver(String classPath) {
        this.classPath = classPath;
        //pinpoint-bootstrap.xxx.jar
        this.agentPattern = DEFAULT_AGENT_PATTERN;
        //pinpoint-commons.xxx.jar
        this.agentCommonsPattern = DEFAULT_AGENT_COMMONS_PATTERN;
        //pinpoint-bootstrap-core.xxx.jar
        this.agentCorePattern = DEFAULT_AGENT_CORE_PATTERN;
        //pinpoint-bootstrap-core-optional.xxx.jar
        this.agentCoreOptionalPattern = DEFAULT_AGENT_CORE_OPTIONAL_PATTERN;
        //pinpoint-annotations.xxx.jar
        this.annotationsPattern = DEFAULT_ANNOTATIONS;
        //配置文件后缀.
        this.fileExtensionList = getDefaultFileExtensionList();
    }

    /**
     * 默认的支持文件后缀，jar，xml，properties
     * @return
     */
    public List<String> getDefaultFileExtensionList() {
        List<String> extensionList = new ArrayList<String>();
        extensionList.add("jar");
        extensionList.add("xml");
        extensionList.add("properties");
        return extensionList;
    }

    public AgentDirBaseClassPathResolver(String classPath, String agentPattern) {
        this.classPath = classPath;
        this.agentPattern = Pattern.compile(agentPattern);
        this.agentCommonsPattern = DEFAULT_AGENT_COMMONS_PATTERN;
        this.agentCorePattern = DEFAULT_AGENT_CORE_PATTERN;
        this.agentCoreOptionalPattern = DEFAULT_AGENT_CORE_OPTIONAL_PATTERN;
        this.annotationsPattern = DEFAULT_ANNOTATIONS;
        this.fileExtensionList = getDefaultFileExtensionList();
    }

    /**
     * 对agent进行简单验证，
     * 主要是验证agent的boot目录下的jar包是否存在，
     * 包括必要的和可选的jar包。
     * @return
     */
    @Override
    public boolean verify() {

        final BootstrapJarFile bootstrapJarFile = new BootstrapJarFile();

        // 1st find boot-strap.jar
        // 第一步定位pinpoint-bootstrap是否存在,该类是javaagent指点使用的类
        final boolean agentJarNotFound = this.findAgentJar();
        if (!agentJarNotFound) {
            logger.warn("pinpoint-bootstrap-x.x.x(-SNAPSHOT).jar not found.");
            return false;
        }

        // 2nd find pinpoint-commons.jar
        // 第二步找到pinpoint-commons.jar位于agent同级目录boot下
        final String pinpointCommonsJar = getPinpointCommonsJar();
        if (pinpointCommonsJar == null) {
            logger.warn("pinpoint-commons-x.x.x(-SNAPSHOT).jar not found");
            return false;
        }
        final JarFile pinpointCommonsJarFile = getJarFile(pinpointCommonsJar);
        if (pinpointCommonsJarFile == null) {
            logger.warn("pinpoint-commons-x.x.x(-SNAPSHOT).jar not found");
            return false;
        }
        bootstrapJarFile.append(pinpointCommonsJarFile);

        // 3rd find bootstrap-core.jar
        // 第三步找到pinpoint-core.jar位于agent同级目录boot下
        final String bootStrapCoreJar = getBootStrapCoreJar();
        if (bootStrapCoreJar == null) {
            logger.warn("pinpoint-bootstrap-core-x.x.x(-SNAPSHOT).jar not found");
            return false;
        }
        JarFile bootStrapCoreJarFile = getJarFile(bootStrapCoreJar);
        if (bootStrapCoreJarFile == null) {
            logger.warn("pinpoint-bootstrap-core-x.x.x(-SNAPSHOT).jar not found");
            return false;
        }
        bootstrapJarFile.append(bootStrapCoreJarFile);

        // 4th find bootstrap-core-optional.jar
        // 第四步找到pinpoint-core-optional.jar位于agent同级目录boot下
        final String bootStrapCoreOptionalJar = getBootStrapCoreOptionalJar();
        if (bootStrapCoreOptionalJar == null) {
            logger.info("pinpoint-bootstrap-core-optional-x.x.x(-SNAPSHOT).jar not found");
        } else {
            JarFile bootStrapCoreOptionalJarFile = getJarFile(bootStrapCoreOptionalJar);
            if (bootStrapCoreOptionalJarFile == null) {
                logger.info("pinpoint-bootstrap-core-optional-x.x.x(-SNAPSHOT).jar not found");
            } else {
                bootstrapJarFile.append(bootStrapCoreOptionalJarFile);
            }
        }

        // 5th find annotations.jar : optional dependency
        // 第五步找到pinpoint-annotations.jar位于agent同级目录boot下
        final String annotationsJar = getAnnotationsJar();
        if (annotationsJar == null) {
            logger.info("pinpoint-annotations-x.x.x(-SNAPSHOT).jar not found");
        } else {
            JarFile jarFile = getJarFile(annotationsJar);
            bootstrapJarFile.append(jarFile);
        }

        this.bootstrapJarFile = bootstrapJarFile;
        return true;
    }

    public void setClassPathFromSystemProperty() {
        this.classPath = getClassPathFromSystemProperty();
    }

    @Override
    public BootstrapJarFile getBootstrapJarFile() {
        return bootstrapJarFile;
    }

    public static String getClassPathFromSystemProperty() {
        return System.getProperty("java.class.path");
    }

    /**
     * 找到真正有-javaagent:xxx.jar对应的xxx.jar
     * @return
     */
    boolean findAgentJar() {
        Matcher matcher = agentPattern.matcher(classPath);
        if (!matcher.find()) {
            return false;
        }
        this.agentJarName = parseAgentJar(matcher);
        this.agentJarFullPath = parseAgentJarPath(classPath, agentJarName);
        if (agentJarFullPath == null) {
            return false;
        }
        this.agentDirPath = parseAgentDirPath(agentJarFullPath);
        if (agentDirPath == null) {
            return false;
        }

        logger.info("Agent original-path:" + agentDirPath);
        // defense alias change
        this.agentDirPath = toCanonicalPath(agentDirPath);
        logger.info("Agent canonical-path:" + agentDirPath);


        this.pinpointCommonsJar = findFromBootDir("pinpoint-commons.jar", agentCommonsPattern);
        this.bootStrapCoreJar = findFromBootDir("pinpoint-bootstrap-core.jar", agentCorePattern);
        this.bootStrapCoreOptionalJar = findFromBootDir("pinpoint-bootstrap-core-optional.jar", agentCoreOptionalPattern);
        this.annotationsJar = findFromBootDir("pinpoint-annotations.jar", annotationsPattern);
        return true;
    }

    private String toCanonicalPath(String path) {
        final File file = new File(path);
        return toCanonicalPath(file);
    }

    private String toCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            logger.warn(file.getPath() + " getCanonicalPath() error. Error:" + e.getMessage(), e);
            return file.getAbsolutePath();
        }
    }

    private String findFromBootDir(final String name, final Pattern pattern) {
        String bootDirPath = agentDirPath + File.separator + "boot";
        final File[] files = listFiles(name, pattern, bootDirPath);
        if (isEmpty(files)) {
            logger.info(name + " not found.");
            return null;
        } else if (files.length == 1) {
            File file = files[0];
            return toCanonicalPath(file);
        } else {
            logger.info("too many " + name + " found. " + Arrays.toString(files));
            return null;
        }
    }

    private boolean isEmpty(File[] files) {
        return files == null || files.length == 0;
    }

    private File[] listFiles(final String name, final Pattern pattern, String bootDirPath) {
        File bootDir = new File(bootDirPath);
        return bootDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String fileName) {
                Matcher matcher = pattern.matcher(fileName);
                if (matcher.matches()) {

                    logger.info("found " + name + ". " + dir.getAbsolutePath() + File.separator + fileName);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public String getPinpointCommonsJar() {
        return pinpointCommonsJar;
    }

    @Override
    public String getBootStrapCoreJar() {
        return bootStrapCoreJar;
    }

    @Override
    public String getBootStrapCoreOptionalJar() {
        return bootStrapCoreOptionalJar;
    }

    public String getAnnotationsJar() {
        return annotationsJar;
    }

    private String parseAgentJar(Matcher matcher) {
        int start = matcher.start();
        int end = matcher.end();
        return this.classPath.substring(start, end);
    }

    @Override
    public String getAgentJarName() {
        return this.agentJarName;
    }

    private String parseAgentJarPath(String classPath, String agentJar) {
        String[] classPathList = classPath.split(File.pathSeparator);
        for (String findPath : classPathList) {
            boolean find = findPath.contains(agentJar);
            if (find) {
                return findPath;
            }
        }
        return null;
    }

    @Override
    public String getAgentJarFullPath() {
        return agentJarFullPath;
    }

    @Override
    public String getAgentLibPath() {
        return this.agentDirPath + File.separator + "lib";
    }

    @Override
    public String getAgentLogFilePath() {
        return this.agentDirPath + File.separator + "log";
    }

    @Override
    public String getAgentPluginPath() {
        return this.agentDirPath + File.separator + "plugin";
    }

    @Override
    public List<URL> resolveLib() {
        String agentLibPath = getAgentLibPath();
        File libDir = new File(agentLibPath);
        if (!libDir.exists()) {
            logger.warn(agentLibPath + " not found");
            return Collections.emptyList();
        }
        if (!libDir.isDirectory()) {
            logger.warn(agentLibPath + " not Directory");
            return Collections.emptyList();
        }
        final List<URL> jarURLList =  new ArrayList<URL>();

        final File[] findJarList = findJar(libDir);
        if (findJarList != null) {
            for (File file : findJarList) {
                URL url = toURI(file);
                if (url != null) {
                    jarURLList.add(url);
                }
            }
        }

        URL agentDirUri = toURI(new File(agentLibPath));
        if (agentDirUri != null) {
            jarURLList.add(agentDirUri);
        }

        // hot fix. boot jars not found from classPool ??
        jarURLList.add(toURI(new File(getPinpointCommonsJar())));
        jarURLList.add(toURI(new File(getBootStrapCoreJar())));
        String bootstrapCoreOptionalJar = getBootStrapCoreOptionalJar();
        // bootstrap-core-optional jar is not required and is okay to be null
        if (bootstrapCoreOptionalJar != null) {
            jarURLList.add(toURI(new File(bootstrapCoreOptionalJar)));
        }

        return jarURLList;
    }

    /**
     * 解析插件，并返回相应的url
     * @return
     */
    @Override
    public URL[] resolvePlugins() {
        //插件位于agent同级下lib目录，也就是这个File的含义
        final File file = new File(getAgentPluginPath());
        
        if (!file.exists()) {
            logger.warn(file + " not found");
            return new URL[0];
        }
        
        if (!file.isDirectory()) {
            logger.warn(file + " is not a directory");
            return new URL[0];
        }
        
        //筛选出相应的jar包
        final File[] jars = file.listFiles(new FilenameFilter() {
            
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });

        if (isEmpty(jars)) {
            return new URL[0];
        }
        
        final URL[] urls = new URL[jars.length];
        
        
        for (int i = 0; i < jars.length; i++) {
            try {
                urls[i] = jars[i].toURI().toURL();
            } catch (MalformedURLException e) {
                // TODO have to change to PinpointException AFTER moving the exception to pinpoint-common
                throw new RuntimeException("Fail to load plugin jars", e);
            }
        }

        for (File pluginJar : jars) {
            logger.info("Found plugins: " + pluginJar.getPath());
        }

        return urls;
    }

    private URL toURI(File file) {
        URI uri = file.toURI();
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            logger.warn(file.getName() + ".toURL() failed.", e);
            return null;
        }
    }

    private File[] findJar(File libDir) {
        return libDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String path = pathname.getName();
                for (String extension : fileExtensionList) {
                    if (path.lastIndexOf("." + extension) != -1) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private String parseAgentDirPath(String agentJarFullPath) {
        int index1 = agentJarFullPath.lastIndexOf("/");
        int index2 = agentJarFullPath.lastIndexOf("\\");
        int max = Math.max(index1, index2);
        if (max == -1) {
            return null;
        }
        return agentJarFullPath.substring(0, max);
    }

    @Override
    public String getAgentDirPath() {
        return agentDirPath;
    }

    @Override
    public String getAgentConfigPath() {
        return agentDirPath + File.separator + "pinpoint.config";
    }


    private JarFile getJarFile(String jarFilePath) {
        try {
            return new JarFile(jarFilePath);
        } catch (IOException ioe) {
            logger.warn(jarFilePath + " file not found. Error:" + ioe.getMessage(), ioe);
            return null;
        }
    }


}
