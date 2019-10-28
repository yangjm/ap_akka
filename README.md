# About
Integrates Akka Actor with Spring Framework

# Usage

1. Add annotation @Component and @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE) to actor classes
2. Use @Import to import AkkaConfig.class. Make sure actor classes are in package scanned by Spring
3. In the main() function of the application, before calling SpringApplication.run(), call AkkaConfig.processArgs() to process command line arguments
~~~~
    public static void main(String[] args) {
        AkkaConfig.processArgs(args);
        SpringApplication.run(SchedulerApplication.class, args);
    }
~~~~
4. Inject ActorCreator to classes that need to create Akka actor. Use ActorCreator.create() to create container managed actor instance.
5. ActorSystem is managed by Spring container, it can be injected by @Autowired

# Configuration
1. Create application.conf in the resources directory in project
2. Configure akka.system.name and akka.conf in project's application.properties or application.yml. For example:
        akka.system.name=akkaapp
        akka.conf=akka.remote.netty.tcp.port=5000;akka.cluster.seed-nodes=["akka.tcp://akkaapp@127.0.0.1:5000"];akka.cluster.roles=[app]

# Command Line Parameters
1. Use -port to specify AkkaSystem listen port, for example -port1234
2. Use -host to specify AkkaSystem binding host, for example -hostcore
