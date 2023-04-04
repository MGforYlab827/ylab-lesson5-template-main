package io.ylab.intensive.lesson05.eventsourcing.api;

import io.ylab.intensive.lesson05.eventsourcing.Person;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class ApiApp {
  public static void main(String[] args) throws Exception {
    // Тут пишем создание PersonApi, запуск и демонстрацию работы
    AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(Config.class);
    applicationContext.start();
    PersonApi personApi = applicationContext.getBean(PersonApi.class);
    // пишем взаимодействие с PersonApi
    personApi.deletePerson(100L);
    personApi.savePerson(1L, "Darya", "Ivanova", "Iv");
    personApi.savePerson(2L, "Maria", "M", "Ma");
    personApi.savePerson(3L, "Margo", "G", "Ge");
    Thread.sleep(2000);
    Person person = personApi.findPerson(1L);
    showPerson(person);
    System.out.println("=================");
    for (Person person1 : personApi.findAll()){
      showPerson(person1);
    }
    System.out.println("==================");
    personApi.deletePerson(2L);
    Thread.sleep(2000);
    for (Person person1 : personApi.findAll()){
      showPerson(person1);
    }
    applicationContext.close();
  }

  private static void showPerson(Person person){
    if(person != null) {
      System.out.println(person.getId() + " " + person.getName() + " " + person.getLastName() + " " + person.getMiddleName());
    }
  }
}
