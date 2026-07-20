package lan.chaos;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class SimpleAgent {
    /*
       /d/opt/jdk/jdk1.8.0_421/bin/javac.exe SimpleAgent.java
       /d/opt/jdk/jdk1.8.0_421/bin/jar.exe cmf ../../../../../../main/resources/META-INF/MANIFEST.MF agent.jar SimpleAgent.class SimpleAgent\$1.class
      java -javaagent:agent.jar=hello -jar your-app.jar
     */
    public static void premain(String args, Instrumentation inst) {
        System.out.println("[Agent] premain called, args: " + args);

        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(
                    ClassLoader loader,
                    String className,
                    Class<?> classBeingRedefined,
                    ProtectionDomain domain,
                    byte[] classfileBuffer) {

                System.out.println("[Agent] Loading class: " + className);
                return classfileBuffer;
            }
        });
    }
}