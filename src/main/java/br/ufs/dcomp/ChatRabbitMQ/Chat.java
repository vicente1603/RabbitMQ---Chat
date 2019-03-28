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
import com.google.protobuf.ByteString;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.net.URLConnection;
import java.util.HashMap;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.*;


public class Chat {

  private static String prompt = ">>";
  private static String emissor;
  private static String receptor = "";
  private static String mensagem = "";
  private static byte[] mensagemEnvioAB;
  private static String grupo = "";
  static int i = 0;

  public static void main(String[] argv) throws Exception {

    /*
    System.out.println("main() started");
    Thread thread = new Thread(new PrintingRunnable(1));
    thread.start();
    thread.join();
    System.out.println("main() finished");
    */

    //Criando a conexão
    ConnectionFactory factory = new ConnectionFactory();
    //factory.setUri("amqp://adclapft:3xYe7a-bU4zTUjwrJ9DXVemXfkqTk-G3@toad.rmq.cloudamqp.com/adclapft"); // cloudamqp
    factory.setUri("amqp://admin:admin@ec2-3-87-228-250.compute-1.amazonaws.com/aws"); // aws 
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    Channel channel_arquivo = connection.createChannel();

    //Obtendo a primeira entrada do usuário
    System.out.println("Usuário: ");
    Scanner sc = new Scanner(System.in);
    emissor = sc.nextLine();

    //Criando a fila do usuário emissor
    channel.queueDeclare(emissor, false,   false,     false,       null);

    //Criando a fila de arquivos do usuário emissor
    channel_arquivo.queueDeclare(emissor, false,   false,     false,       null);
    
    //Começando o prompt de mensagens
    System.out.println("");
    System.out.print(prompt);

    while (true) {

      //Lendo o que foi digitado
      String mensagem = sc.nextLine();
      String tipoMensagem = "texto simples";

      montarMensagemEnvio(mensagem,tipoMensagem);
      
      if (mensagem.startsWith("&")) {

        System.out.println("");
        System.out.println("----------------------------------------------------------------------------------------------------");
        System.out.println("|                  COMANDO                          |                  DESCRIÇÃO                   |");
        System.out.println("----------------------------------------------------------------------------------------------------");
        System.out.println("| @nomeUsuario                                      |  Mandar mensagem para um usuário específico  |");
        System.out.println("| #nomeGrupo                                        |  Mandar mensagem para um grupo específico    |");
        System.out.println("| !addGroup     <nome-do-grupo>                     |  Criar um grupo                              |");
        System.out.println("| !removeGroup  <nome-do-grupo>                     |  Apagar um grupo                             |");
        System.out.println("| !addUser      <nome-do-usuario> <nome-do-grupo>   |  Adicionar um usuário a um grupo específico  |");
        System.out.println("| !delFromGroup <nome-do-usuario> <nome-do-grupo>   |  Remover um usuário de um grupo específico   |");
        System.out.println("| !upload       <caminho-do-arquivo>                |  Fazer upload de um arquivo                  |");
        System.out.println("| !listGroups                                       |  Lista os grupos do usuário logado           |");
        System.out.println("| !listUsers    <nome-do-grupo>                     |  Listar os usuários do grupo específico      |");
        System.out.println("----------------------------------------------------------------------------------------------------");
        System.out.println("");
        
        System.out.print(receptor + prompt);

      }else if (mensagem.startsWith("@")) {

        //Mudando o receptor para um usuário específico
        mudarUsuarioReceptor(channel, channel_arquivo, mensagem);

      } else if (mensagem.startsWith("#")) {

        //Mudando o receptor para um grupo específico
        mudarGrupoReceptor(channel, channel_arquivo, mensagem);

      } else if (mensagem.startsWith("!")) {

        if (mensagem.startsWith("!addGroup")) {
          
          //Criando um grupo
          criarGrupo(channel, channel_arquivo, mensagem);

        } else if (mensagem.startsWith("!removeGroup")) {

          //Apagando um grupo
          removerGrupo(channel, channel_arquivo, mensagem);

        } else if (mensagem.startsWith("!addUser")) {

          //Adicionando um usuário a um grupo
          adicionarUsuarioAGrupo(channel, channel_arquivo, mensagem);
            
        } else if (mensagem.startsWith("!delFromGroup")) {

          //Removendo um usuário de um grupo
          removerUsuarioDeGrupo(channel, channel_arquivo, mensagem);

        } else if (mensagem.startsWith("!upload")) {

          //Fazendo um upload de arquivo
          tipoMensagem = "arquivo";
          montarMensagemEnvio(mensagem, tipoMensagem);

        } else if (mensagem.startsWith("!listUsers")){
          
          //Listando os usuários de um grupo específico
          listarUsuariosGrupo(channel, mensagem);
          
        } else if (mensagem.startsWith("!listGroups")){
          
          //Listando todos os grupos criados até então
          listarGruposUsuarios();
          
        } else {

            System.out.println("Comando não encontrado!");
            System.out.println("");
            System.out.print(prompt);

        }

      }else if(!receptor.equals("")){

        System.out.print(receptor + prompt); 
        
        //Enviando as mensagens na fila do receptor
        channel.basicPublish("",       receptor, null,  mensagemEnvioAB);
        channel.queueDeclare(receptor, false,   false,     false,       null);

      }else if(!grupo.equals("")){

        System.out.print("#" + grupo + prompt);
          
        try{
          
          //Enviando as mensagens do grupo
          channel.basicPublish(grupo, "", null,  mensagemEnvioAB);

        }catch (IOException ex) {

          System.out.println(ex);
    
        }

      }else{

        System.out.println("Digite '@' e em seguida o nome do usuário para enviar uma mensagem ou digite '&' para saber os comandos disponíveis.");
        System.out.println("");
        System.out.print(prompt);

      }

      if (mensagem.startsWith("!upload")) {

        //Envindo as mensagens da fila do receptor
        //channel.basicPublish("",       "@" + receptor, null,  mensagemEnvioAB);
        //channel.queueDeclare("@" + receptor, false,   false,     false,       null);
        
        Thread thread = new Thread(new UploadArquivo(channel_arquivo, mensagemEnvioAB, receptor));
        thread.start();

        if(!grupo.equals("")){
          //Envindo os arquivos do grupo
          channel_arquivo.basicPublish(grupo, "", null,  mensagemEnvioAB);
        }

      }

    }

  }

  private static void mudarUsuarioReceptor(Channel channel, Channel channel_arquivo, String mensagem) throws IOException {

    //Mudando o layout do prompt para "nomeUsuario>>"
    receptor = mensagem;
    receptor = receptor.replace("@", "");
    System.out.println("As próximas mensagens serão enviadas para " + receptor + ".");
    System.out.println("");
    System.out.print(receptor + prompt);

    Consumer consumer = new DefaultConsumer(channel) {
          
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
      
        MensagemProto.Mensagem mensagemRecebida = MensagemProto.Mensagem.parseFrom(body);

        if(mensagemRecebida.getConteudo().getTipo() == ""){

          System.out.println("\n("+ mensagemRecebida.getData() + " às "+ mensagemRecebida.getHora() + ") " + mensagemRecebida.getEmissor() + " diz: " + mensagemRecebida.getConteudo().getCorpo().toString("UTF-8"));

        }else{

          try {

            ByteString mensagemRecebidaBS = mensagemRecebida.getConteudo().getCorpo();
            byte[] mensagemRecebidaAB = mensagemRecebidaBS.toByteArray();

            String fileDest = mensagemRecebida.getConteudo().getNome(); 

            Path path = Paths.get(fileDest);
            Files.write(path, mensagemRecebidaAB);

            System.out.println("O arquivo " + mensagemRecebida.getConteudo().getNome() + " foi baixado no caminho " + fileDest);

          } catch (IOException e) {

            e.printStackTrace();

          }

        }

        System.out.println(receptor + prompt);
        
      }

    };

    //Consumindo a fila do emissor    
    channel.basicConsume(emissor, true, consumer); 

    //Consumindo a fila de arquivos do emissor    
    channel_arquivo.basicConsume(emissor, true, consumer); 

  }

  private static void mudarGrupoReceptor(Channel channel, Channel channel_arquivo, String mensagem) throws IOException {

    //Mudando o layout do prompt para "#nomeGrupo>>"
    receptor = "";    
    grupo = mensagem;
    grupo = grupo.replace("#", "");

    System.out.println("As próximas mensagens serão enviadas para o grupo " + grupo + ".");
    System.out.println("");
    System.out.print("#" + grupo + prompt);
    
    Consumer consumer = new DefaultConsumer(channel) {
    
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        
        MensagemProto.Mensagem mensagemRecebida = MensagemProto.Mensagem.parseFrom(body);

        if (!emissor.equals(mensagemRecebida.getEmissor()))
        
        if(mensagemRecebida.getConteudo().getTipo() == ""){

          System.out.println("("+ mensagemRecebida.getData() + " às "+ mensagemRecebida.getHora() + ") " + mensagemRecebida.getEmissor() + "#" + grupo + " diz: " + mensagemRecebida.getConteudo().getCorpo().toString("UTF-8"));

        }else{

          try {

            ByteString mensagemRecebidaBS = mensagemRecebida.getConteudo().getCorpo();
            byte[] mensagemRecebidaAB = mensagemRecebidaBS.toByteArray();

            String fileDest = mensagemRecebida.getConteudo().getNome(); 

            Path path = Paths.get(fileDest);
            Files.write(path, mensagemRecebidaAB);

            System.out.println("O arquivo " + mensagemRecebida.getConteudo().getNome() + " foi baixado no caminho " + fileDest);

          } catch (IOException e) {

            e.printStackTrace();

          }

        }
        
            System.out.println(receptor + prompt);

      }

    };

    //Consumindo a fila do emissor    
    channel.basicConsume(emissor, true, consumer); 

    //Consumindo a fila de arquivos do emissor    
    channel_arquivo.basicConsume(emissor, true, consumer); 

  }

  private static void criarGrupo(Channel channel, Channel channel_arquivo, String mensagem) throws IOException {
    
    String texto[] = mensagem.split(" ");    
    grupo = texto[1];
    
    if(grupo.equals("")){

      System.out.println("Digite o nome do grupo que deseja criar.");

    }else{
      
      //Criando o grupo
      channel.exchangeDeclare(grupo.trim(), "fanout");

      //Criando o grupo de arquivos
      channel_arquivo.exchangeDeclare(grupo.trim(), "fanout");
      
      //Adicionando o emissor ao grupo
      channel.queueBind(emissor, grupo.trim(), "");

      //Adicionando o emissor ao grupo
      channel_arquivo.queueBind(emissor, grupo.trim(), "");
        
      System.out.println("Grupo " + grupo + " criado com sucesso.");
      System.out.println("");

      System.out.print(receptor + prompt);

    }

  }

  private static void listarUsuariosGrupo(Channel channel, String mensagem) throws IOException{
    
    try {
      
      String nomeServico = "listarUsuariosGrupo";
      String dadoARecuperar = "usuariosGrupo";
      
      Response resposta = recuperarJson(mensagem, nomeServico);
   
      if (resposta.getStatus() == 200) {
      
        extrairEListarDadosJson(formatarJson(resposta), dadoARecuperar);
        System.out.print("\n" + prompt);
      
      }    
            
		} catch (Exception e) {

      e.printStackTrace();
      
		}
    
  }
  
  private static void listarGruposUsuarios() throws IOException{
    
    try {

      String nomeServico = "listarGruposUsuarios";
      String dadoARecuperar = "grupos";
      
      Response resposta = recuperarJson(mensagem,nomeServico);
      
      if (resposta.getStatus() == 200) {
      
        extrairEListarDadosJson(formatarJson(resposta),dadoARecuperar);
        System.out.print("\n" + prompt);
      
      }    
            
		} catch (Exception e) {

      e.printStackTrace();
      
		}
    
  }
  
  private static Response recuperarJson(String mensagem, String nomeServico){

    String caminhoServico = "";
    Response resposta = null;

    if(nomeServico == "listarUsuariosGrupo"){

      String texto[] = mensagem.split(" ");    
      grupo = texto[1];
      caminhoServico = "/api/exchanges/aws/" + grupo + "/bindings/source";

    }else if (nomeServico == "listarGruposUsuarios"){

      caminhoServico = "/api/queues/aws/" + emissor + "/bindings";

    }

    //Preparando os dados para a requisição
    String username = "admin";
    String password = "admin";
    String usernameAndPassword = username + ":" + password;
    String authorizationHeaderName = "Authorization";
    String authorizationHeaderValue = "Basic " + java.util.Base64.getEncoder().encodeToString( usernameAndPassword.getBytes() );

    if(caminhoServico != ""){

      //Realizando a requisição
      String restResource = "http://Balancer2-4494417c2cd96b89.elb.us-east-1.amazonaws.com:80";
      Client client = ClientBuilder.newClient();
      resposta = client.target( restResource )
        .path(caminhoServico) // a requisição depende do caminho do serviço
        .request(MediaType.APPLICATION_JSON)
          .header( authorizationHeaderName, authorizationHeaderValue ) 
          .get();    

    }

    return resposta;

  }

  private static String formatarJson(Response resposta){

    String jsonData = resposta.readEntity(String.class);
        
    jsonData =  "{\"bindings\": "+ 
                  "[" +
                    "{\"ircEvent\":"
                      + jsonData
                    + "}"
                  + "]"
                + "}";
    
    return jsonData;

  }

  private static void extrairEListarDadosJson(String jsonData, String dadoARecuperar){

    try {

      JSONObject jObj = new JSONObject( jsonData );
      JSONArray jArray = jObj.getJSONArray( "bindings" );

      for ( int i = 0; i < jArray.length(); i++ ) {

        JSONObject jo = jArray.getJSONObject( i );
        JSONArray jArrayIrcEvent = jo.optJSONArray( "ircEvent" );

        for (int j = 0; j < jArrayIrcEvent.length(); j++) {

          JSONObject joi = jArrayIrcEvent.getJSONObject(j);

          if (dadoARecuperar == "usuariosGrupo"){

            if (joi.has("destination")) {

              System.out.println(joi.getString("destination"));

            }

          }else if (dadoARecuperar == "grupos"){

            if (joi.has("source")) {

              if(!joi.getString("source").equals("")){
                System.out.println(joi.getString("source"));
              }

            }

          }

        }
      }

    } catch ( JSONException exc ) {
    
      exc.printStackTrace();

    }

  }

  private static void removerGrupo(Channel channel, Channel channel_arquivo, String mensagem) throws IOException {

    String texto[] = mensagem.split(" ");
    grupo = texto[1];
    
    if(grupo.equals("")){

      System.out.println("Digite o nome do grupo que deseja remover");

    }else{
      
      //Removendo o grupo
      channel.exchangeDelete(grupo.trim());

      //Removendo o grupo
      channel_arquivo.exchangeDelete(grupo.trim());
    
      System.out.println("Grupo " + grupo.trim() + " removido com sucesso.");
      System.out.println("");

      System.out.print(receptor + prompt);

    }

  }

  private static void adicionarUsuarioAGrupo(Channel channel, Channel channel_arquivo, String mensagem) throws IOException {

    String text[] = mensagem.split(" ");
            
    receptor = text[1];
    grupo = text[2];
    
    //Adicionando o usuário ao grupo
    channel.queueBind(receptor, grupo, "");

    //Adicionando o usuário ao grupo
    channel_arquivo.queueBind(receptor, grupo, "");
    
    System.out.println(receptor + " foi adicionado ao grupo " + grupo);

    System.out.print(prompt);

  }
        
  private static void removerUsuarioDeGrupo(Channel channel, Channel channel_arquivo, String mensagem) throws IOException {

    String text[] = mensagem.split(" ");
            
    receptor = text[1];
    grupo = text[2];
    
    //Removendo o usuário do grupo
    channel.queueUnbind(receptor, grupo, "");

    //Removendo o usuário do grupo
    channel_arquivo.queueUnbind(receptor, grupo, "");
    
    System.out.println(receptor + " foi removido do grupo " + grupo); 

    System.out.print(prompt);

  }

  private static void montarMensagemEnvio(String mensagem, String tipoMensagem) throws IOException{

    ByteString mensagemEmByteString = null;
    String tipoMime = "";
    String nome = "";

    if (tipoMensagem == "arquivo"){

      String caminho = mensagem.replace("!upload ", "");
      Path path = Paths.get(caminho);
  
      if (path.toFile().exists()) {

        File file = new File(caminho);

        tipoMime = URLConnection.guessContentTypeFromName(file.getName());

        byte[] arquivoBA = new byte[(int) file.length()]; 

        FileInputStream fis = new FileInputStream(file);
        fis.read(arquivoBA);
        fis.close();

        ByteString arquivoBS = ByteString.copyFrom(arquivoBA);

        mensagemEmByteString = arquivoBS;

        nome = file.getName();

        System.out.println("Enviando " +file.getName()+ " para " + receptor);
        
        System.out.println(prompt);
      
      } else {

        System.out.println("Arquivo não existe!");

      }
      
        Runnable t2 = () -> {
        //mensagem = receptor;
        //mudarUsuarioReceptor(channel_1, mensagem, extensoes);
        //metodo1();
        };
        
        Thread thread2 = new Thread(t2);
        
        thread2.start();

    }else{

      mensagemEmByteString = ByteString.copyFrom(mensagem.getBytes()); //Transformando a String em um ByteString

    }
    
    //Recuperando e formatando a data atual
    Date dataAtual = new Date(System.currentTimeMillis());  
    SimpleDateFormat formatoData = new SimpleDateFormat("dd/MM/yyyy"); 
    
    //Recuperando e formatando a hora atual
    Date horaAtual = new  Date(System.currentTimeMillis());
    SimpleDateFormat formatoHora = new SimpleDateFormat("HH:mm");

    //Montando o builder de Conteudo
    MensagemProto.Conteudo.Builder builderConteudo = MensagemProto.Conteudo.newBuilder();
    builderConteudo.setTipo(tipoMime);
    builderConteudo.setCorpo(mensagemEmByteString); 
    builderConteudo.setNome(nome);

    //Montando o builder de Mensagem
    MensagemProto.Mensagem.Builder builderMensagem = MensagemProto.Mensagem.newBuilder();
    builderMensagem.setEmissor(emissor);
    builderMensagem.setData(formatoData.format(dataAtual));
    builderMensagem.setHora(formatoHora.format(horaAtual));
    builderMensagem.setGrupo(grupo);
    builderMensagem.setConteudo(builderConteudo);

    //Buildando a mensagem que será enviada no formato MensagemProto.Mensagem
    MensagemProto.Mensagem mensagemEnvioMPM = builderMensagem.build();

    //Transformando de MensagemProto.Mensagem para um array de bytes que será usado no método basicPublish
    mensagemEnvioAB = mensagemEnvioMPM.toByteArray();
    
  }

}

class UploadArquivo implements Runnable {

  private final Channel channel_arquivo;
  private final byte[] mensagemEnvioAB;
  private final String receptor;

  public UploadArquivo(Channel channel_arquivo, byte[] mensagemEnvioAB, String receptor) {
      this.channel_arquivo = channel_arquivo;
      this.mensagemEnvioAB = mensagemEnvioAB;
      this.receptor = receptor;
  }

  @Override
  public void run() {
      try {

          //Envindo as mensagens da fila do receptor
          channel_arquivo.basicPublish("", receptor, null,  mensagemEnvioAB);
          channel_arquivo.queueDeclare(receptor, false,   false,     false,       null);
          
      } catch (Exception ex) {
          System.out.println("Thread encerrada.");
      }
  }

}