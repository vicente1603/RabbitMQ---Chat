package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.io.*;
import com.google.protobuf.util.JsonFormat;

public class Chat {

  private static String prompt = ">>";
  private static String emissor;
  private static String receptor = "";
  private static String mensagem = "";
  private static String grupo = "";

  public static void main(String[] argv) throws Exception {

    //Criando a conexão
    ConnectionFactory factory = new ConnectionFactory();
    factory.setUri("amqp://adclapft:3xYe7a-bU4zTUjwrJ9DXVemXfkqTk-G3@toad.rmq.cloudamqp.com/adclapft"); // cloudamqp
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    //Obtendo a primeira entrada do usuário
    System.out.println("Usuário: ");
    Scanner sc = new Scanner(System.in);
    emissor = sc.nextLine();

    //Criando a fila do usuário emissor
    channel.queueDeclare(emissor, false,   false,     false,       null);
    
    //Começando o prompt de mensagens
    System.out.println("");
    System.out.print(prompt);

    while (true) {

      String mensagem = sc.nextLine();
      
      if (mensagem.startsWith("&")) {

        System.out.println("");
        System.out.println("--------------------------------------------------------------------------------------------");
        System.out.println("|                  COMANDO                  |                  DESCRIÇÃO                   |");
        System.out.println("--------------------------------------------------------------------------------------------");
        System.out.println("| @nomeUsuario                              |  Mandar mensagem para um usuário específico  |");
        System.out.println("| #nomeGrupo                                |  Mandar mensagem para um grupo específico    |");
        System.out.println("| !addGroup     <nomeGrupo>                 |  Criar um grupo                              |");
        System.out.println("| !removeGroup  <nomeGrupo>                 |  Apagar um grupo                             |");
        System.out.println("| !addUser      <nomeUsuario> <nomeGrupo>   |  Adicionar um usuário a um grupo específico  |");
        System.out.println("| !delFromGroup <nomeUsuario> <nomeGrupo>   |  Remover um usuário de um grupo específico  |");
        System.out.println("--------------------------------------------------------------------------------------------");
        System.out.println("");
        
        System.out.print(receptor + prompt);

      }else if (mensagem.startsWith("@")) {

        //Mudando o receptor para um usuário específico
        mudarUsuarioReceptor(channel, mensagem);

      } else if (mensagem.startsWith("#")) {

        //Mudando o receptor para um grupo específico
        mudarGrupoReceptor(channel, mensagem);

      } else if (mensagem.startsWith("!")) {

        if (mensagem.startsWith("!addGroup")) {
          
          //Criando um grupo
          criarGrupo(channel, mensagem);

        } else if (mensagem.startsWith("!removeGroup")) {

          //Apagando um grupo
          removerGrupo(channel, mensagem);

        } else if (mensagem.startsWith("!addUser")) {

          //Adicionado um usuário a um grupo
          adicionarUsuarioAGrupo(channel, mensagem);

        } else if (mensagem.startsWith("!delFromGroup")) {

          //Removendo um usuário de um grupo
          removerUsuarioDeGrupo(channel, mensagem);

        } else {

            System.out.println("Comando não encontrado!");
            System.out.println("");
            System.out.print(prompt);

        }

      }else if(!receptor.equals("")){

        System.out.print(receptor + prompt); 
           
        channel.basicPublish("",       receptor, null,  mensagem.getBytes("UTF-8"));
        channel.queueDeclare(receptor, false,   false,     false,       null);

      }else if(!grupo.equals("")){

        System.out.print("#" + grupo + prompt);
          
        try{
          
          channel.basicPublish(grupo, "", null,  mensagem.getBytes("UTF-8"));

        }catch(Exception e){

          System.out.println(e);

        }

      }else{

        System.out.println("Digite '@' e em seguida o nome do usuário para enviar uma mensagem ou digite '&' para saber os comandos disponíveis.");
        System.out.println("");
        System.out.print(prompt);

      }

    }

  }

  private static void mudarUsuarioReceptor(Channel channel, String mensagem) throws IOException {

    //Mudando o layout do prompt para "nome>>"
    receptor = mensagem;
    receptor = receptor.replace("@", "");
    System.out.println("As próximas mensagens serão enviadas para " + receptor + ".");
    System.out.println("");
    System.out.print(receptor + prompt);

    Consumer consumer = new DefaultConsumer(channel) {
          
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
      
        Date data = new Date(System.currentTimeMillis());  
        SimpleDateFormat formatoData = new SimpleDateFormat("(dd/MM/yyyy"); 
        
        Date hora = new  Date(System.currentTimeMillis());
        SimpleDateFormat formatoHora = new SimpleDateFormat("HH:mm) ");

        String mensagem = new String(body, "UTF-8");
        System.out.println("\n"+formatoData.format(data) + " às "+ formatoHora.format(hora) + receptor +" diz: " + mensagem);
        System.out.print(receptor + prompt);
        
      }

    };

    //Consumindo a fila do emissor    
    channel.basicConsume(emissor, true, consumer); 

  }

  private static void mudarGrupoReceptor(Channel channel, String mensagem) throws IOException {

    //Mudando o layout do prompt para "grupo>>"
    receptor = "";    
    grupo = mensagem;
    grupo = grupo.replace("#", "");

    System.out.println("As próximas mensagens serão enviadas para o grupo " + grupo + ".");
    System.out.println("");
    System.out.print("#" + grupo + prompt);
    
    Consumer consumer = new DefaultConsumer(channel) {
    
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        
        Date data = new Date(System.currentTimeMillis());  
        SimpleDateFormat formatoData = new SimpleDateFormat("(dd/MM/yyyy"); 
      
        Date hora = new  Date(System.currentTimeMillis());
        SimpleDateFormat formatoHora = new SimpleDateFormat("HH:mm) ");
          
        String mensagem = new String(body, "UTF-8");
        System.out.println("\n"+formatoData.format(data) + " às "+ formatoHora.format(hora) + receptor +" diz: " + mensagem);
        
        System.out.print("#" + grupo + prompt);

      }

    };

    //Consumindo a fila do emissor    
    channel.basicConsume(emissor, true, consumer); 

  }

  private static void criarGrupo(Channel channel, String mensagem) throws IOException {
    
    System.out.println("Entrou aqui");

    String texto[] = mensagem.split(" ");    
    grupo = texto[1];
    
    if(grupo.equals("")){

      System.out.println("Digite o nome do grupo que deseja criar.");

    }else{
      
      //Criando o grupo
      channel.exchangeDeclare(grupo.trim(), "fanout");
      
      //Adicionando o emissor ao grupo
      channel.queueBind(emissor, grupo.trim(), "");
        
      System.out.println("Grupo " + grupo + " criado com sucesso.");
      System.out.println("");

      System.out.print(receptor + prompt);

    }

  }

  private static void removerGrupo(Channel channel, String mensagem) throws IOException {

    String texto[] = mensagem.split(" ");
    grupo = texto[1];
    
    if(grupo.equals("")){

      System.out.println("Digite o nome do grupo que deseja remover");

    }else{
      
      //Removendo o grupo
      channel.exchangeDelete(grupo.trim());
    
      System.out.println("Grupo " + grupo.trim() + " removido com sucesso.");
      System.out.println("");

      System.out.print(receptor + prompt);

    }

  }

  private static void adicionarUsuarioAGrupo(Channel channel, String mensagem) throws IOException {

    String text[] = mensagem.split(" ");
            
    receptor = text[1];
    grupo = text[2];
    
    //Adicionando o usuário ao grupo
    channel.queueBind(receptor, grupo, "");
    
    System.out.println(receptor + " foi adicionado ao grupo " + grupo);

    System.out.print(prompt);

  }
        
  private static void removerUsuarioDeGrupo(Channel channel, String mensagem) throws IOException {

    String text[] = mensagem.split(" ");
            
    receptor = text[1];
    grupo = text[2];
    
    //Removendo o usuário do grupo
    channel.queueUnbind(receptor, grupo, "");
    
    System.out.println(receptor + " foi removido do grupo " + grupo); 

    System.out.print(prompt);

  }

  
  public void testeProtocolBuffer () throws IOException {
    
    //Tentando implementar o Protocol Buffer

    MensagemProto.Conteudo.Builder builderConteudo = MensagemProto.Conteudo.newBuilder();
    builderConteudo.setTipo("Qualquer tipo");
   // builderConteudo.setCorpo(mensagem)
    builderConteudo.setNome("Nome da Mensagem");
    
    MensagemProto.Mensagem.Builder builderMensagem = MensagemProto.Mensagem.newBuilder();
    builderMensagem.setEmissor("emissor!!!");
    builderMensagem.setData("data!!");
    builderMensagem.setHora("hora!!");
    builderMensagem.setGrupo("grupo!!");
    builderMensagem.setConteudo(builderConteudo);
    
    MensagemProto.Mensagem contatoMensagem = builderMensagem.build();
    
    byte[] buffer = contatoMensagem.toByteArray();
    
    FileOutputStream fos = new FileOutputStream(new File("mensagem.bin"));
    fos.write(buffer);
    fos.close();
    System.out.println("Contato escrito em formato binário no arquivo \"mensagem.bin\"");
    
    File file = new File("mensagem.bin");
    FileInputStream fis = new FileInputStream(file);
    buffer = new byte[(int) file.length()];
    fis.read(buffer);
    fis.close();
    
    contatoMensagem = MensagemProto.Mensagem.parseFrom(buffer);
    
    String emissor = contatoMensagem.getEmissor();
    //String nome = contatoMensagem.getNome();
   
    System.out.println(emissor);
   // System.out.println(nome); 

  }

}