import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.lang.IllegalStateException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageGrouping {
    public static void main(String[] args) throws NamingException, JMSException, InterruptedException {

        InitialContext initialContext = new InitialContext();
        Queue queue = (Queue) initialContext.lookup("queue/myQueue");
        Map<String, String> recievedMessages = new ConcurrentHashMap<>();

        try (ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory();
             JMSContext jmsContextProducer = cf.createContext();
             JMSContext jmsContextConsumer = cf.createContext()) {
/*
  Создаем получателя и двух слушателей, по итогу, мы убедимся что все сообщения примет первый слушатель, указанный тут
  так как он съест всю группу сообщений, ибо увидит у них одинаковый GroupID
*/
            JMSProducer producer = jmsContextProducer.createProducer();
            JMSConsumer consumer1 = jmsContextConsumer.createConsumer(queue);
            consumer1.setMessageListener(new MyListener("Consumer-1", recievedMessages));
            JMSConsumer consumer2 = jmsContextConsumer.createConsumer(queue);
            consumer2.setMessageListener(new MyListener("Consumer-2", recievedMessages));
/*
  Отправляем 10 сообщений у которых будет один GroupID
*/
            int count = 10;
            TextMessage[] messages = new TextMessage[count];
            for (int i = 0; i < count; i++) {
                messages[i] = jmsContextProducer.createTextMessage("Group-0 message " + i);
                messages[i].setStringProperty("JMSXGroupID", "Group-0");
                producer.send(queue, messages[i]);
            }
            Thread.sleep(2000);

/*
  Проверяем что их все получил первый получатель
*/
            for (TextMessage message : messages) {
                if (!recievedMessages.get(message.getText()).equals("Consumer-1")) {
                    throw new IllegalStateException("Group Message " + message.getText() + " has gone to the wrong reciever");
                }
            }

        }
    }
}


