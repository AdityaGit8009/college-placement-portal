package edu.campus;
import java.nio.file.Path;
import org.apache.catalina.startup.Tomcat;
/** Development-only embedded Tomcat. Production uses the WAR in Docker. */
public class DevServer {public static void main(String[]args)throws Exception{Tomcat t=new Tomcat();t.setBaseDir(Path.of("target/dev-tomcat").toAbsolutePath().toString());t.setPort(Integer.parseInt(Security.config("PORT","8080")));t.getConnector();var c=t.addWebapp("",Path.of("src/main/webapp").toAbsolutePath().toString());c.setParentClassLoader(DevServer.class.getClassLoader());t.start();System.out.println("Launchpad: http://localhost:"+t.getConnector().getPort());t.getServer().await();}}
