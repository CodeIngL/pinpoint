/*
 * Copyright 2016 NAVER Corp.
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
 *
 */

package com.navercorp.pinpoint.bootstrap;

import java.net.URL;
import java.util.List;

/**
 * 路径解析，用于解析相关的类路径
 * @author Woonduk Kang(emeroad)
 */
public interface ClassPathResolver {

    /**
     * 做agent目录中相关的依赖验证,agent是一个目录，会去寻找这个目录完成整个agent的额读取
     *
     * @return false agent 不对，整个agent不生效，true，继续下一步
     */
    boolean verify();

    BootstrapJarFile getBootstrapJarFile();

    String getPinpointCommonsJar();

    String getBootStrapCoreJar();

    String getBootStrapCoreOptionalJar();

    String getAgentJarName();

    String getAgentJarFullPath();

    String getAgentLibPath();

    String getAgentLogFilePath();

    String getAgentPluginPath();

    /**
     * 获得lib目录其中的jar的url
     * @return
     */
    List<URL> resolveLib();

    URL[] resolvePlugins();

    String getAgentDirPath();

    String getAgentConfigPath();
}
