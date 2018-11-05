# pinpoint-bootstrap

该目录产生的jar，是agentjar，用于启动项中-javaagent:xxx.jar


META-INF中内容：ex:

    Manifest-Version: 1.0
    Premain-Class: com.navercorp.pinpoint.bootstrap.PinpointBootStrap
    Archiver-Version: Plexus Archiver
    Built-By: laihj
    Can-Redefine-Classes: true
    Pinpoint-Version: 1.7.4-SNAPSHOT
    Can-Retransform-Classes: true
    Created-By: Apache Maven 3.3.9
    Build-Jdk: 1.8.0_91
    
注意Premain-Class:Pinpoint-Version
