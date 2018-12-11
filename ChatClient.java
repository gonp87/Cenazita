import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class ChatClient {

    // Variáveis relacionadas com a interface gráfica --- * NÃO MODIFICAR *
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    // --- Fim das variáveis relacionadas coma interface gráfica

    // Se for necessário adicionar variáveis ao objecto ChatClient, devem
    // ser colocadas aqui
	String sentence;
	String modifiedSentence;
	BufferedReader inFromUser;
	Socket clientSocket;
	DataOutputStream outToServer;
	BufferedReader inFromServer;


    
    // Método a usar para acrescentar uma string à caixa de texto
    // * NÃO MODIFICAR *
    public void printMessage(final String message) {
        chatArea.append(message);
    }

    
    // Construtor
    public ChatClient(String server, int port) throws IOException {

        // Inicialização da interface gráfica --- * NÃO MODIFICAR *
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    newMessage(chatBox.getText());
                } catch (IOException ex) {
                } finally {
                   chatBox.setText("");
                }
            }
        });
        // --- Fim da inicialização da interface gráfica

        // Se for necessário adicionar código de inicialização ao
        // construtor, deve ser colocado aqui

	inFromUser = new BufferedReader(new InputStreamReader(System.in));
	clientSocket = new Socket(server, port);
	outToServer = new DataOutputStream(clientSocket.getOutputStream());
	inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

    }


    // Método invocado sempre que o utilizador insere uma mensagem
    // na caixa de entrada
    public void newMessage(String message) throws IOException {
        // PREENCHER AQUI com código que envia a mensagem ao servidor

		outToServer.writeBytes(message +'\n');


    }

    
    // Método principal do objecto
    public void run() throws IOException {
        // PREENCHER AQUI
	boolean go=true;
	while(go)
	    {
		//if((sentence = inFromUser.readLine()) == null)
		//    break;
	        modifiedSentence = inFromServer.readLine();
		String parts[] = modifiedSentence.split(" ",3);
		try {
		    switch (parts[0]) {
		    case "OK":
			printMessage("OK\n");
			break;
		    case "ERROR":
			printMessage("ERROR\n");
			break;
		    case "MESSAGE":
			printMessage(parts[1] + ": " + parts[2] + "\n");
			break;
		    case "NEWNICK":
			printMessage(parts[1] + " mudou de nome para " + parts[2]+"\n");
			break;
		    case "JOINED":
			printMessage(parts[1] + " entrou na sala\n");
			break;
		    case "LEFT":
			printMessage(parts[1] + " saiu da sala\n");
			break;
		    case "BYE":
			go=false;
			break;
		    case "PRIVATE":
			printMessage(parts[1] + " (private): " + parts[2] + "\n");
			break;
		    default:
			throw new IOException("Something went wrong\n");
		    }
		} catch (IOException ex) {
		    System.out.println(ex.getMessage());
		}
	    }
	clientSocket.close();
    }
    

    // Instancia o ChatClient e arranca-o invocando o seu método run()
    // * NÃO MODIFICAR *
    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }
}
