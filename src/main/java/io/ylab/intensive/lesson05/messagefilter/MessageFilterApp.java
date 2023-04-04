package io.ylab.intensive.lesson05.messagefilter;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class MessageFilterApp {
  public static void main(String[] args) throws Exception {
    AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(Config.class);
    applicationContext.start();
    MessageFilter messageFilter = applicationContext.getBean(MessageFilter.class);
    messageFilter.startMessageFilter();
    applicationContext.close();
  }
}
