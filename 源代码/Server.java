import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TreeMap;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;


public class Server {
    public static boolean flagEnd = false;
    static Inspection inspection = null;
    static Control control = null;
    static Anteroom anteroom = null;

   public  static TreeMap<String, Link> links = new TreeMap<>();
   public  static ConcurrentLinkedDeque<String> messages = new ConcurrentLinkedDeque<>();
    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    static ServerSocket serverSocket=null;

    public static void main(String[] args) {
        inspection = new Inspection();
        control = new Control();
        anteroom = new Anteroom();

        if (args.length != 1) {
            System.err.println("必须给出日志文件路径");
            return;
        }

        String logFilePath = args[0];
        File logFile = new File(logFilePath);
        if (!logFile.exists()) {
            System.err.println("您给出的日志文件不存在，请查看");
            return;
        }

        if (Log.createWriter(logFilePath)) {
            System.out.println("日志文件流创建成功，可以接收系统运行日志了，全民大聊天正式开始！");
            Log.append("["+ LocalDateTime.now().format(Server.formatter)+"]"+"系统启动---");
        } else {
            System.err.println("日志文件流创建失败，请检查日志文件路径");
            return;
        }
        inspection.start();
        control.start();
        anteroom.start();
        System.out.println("如果想结束聊天服务器请输入指令:end");

    }
}

class Inspection extends Thread {
    public void run() {
        while (!Server.flagEnd) {
         String message = Server.messages.pollFirst();
         if (message != null) {
             synchronized (Server.links) {
                 for (Map.Entry<String, Link> entry : Server.links.entrySet()) {
                     entry.getValue().sender.messagesToSend.add(message);
                 }
             }
             System.out.println(message);
         }
     }
     }
}

class Control extends Thread {
    static BufferedReader keyboardInput = null;
    static {
        keyboardInput = new BufferedReader(new InputStreamReader(System.in));
    }

    public void run() {
        String input;
        try {
            while ((input = keyboardInput.readLine()) != null) {
                if (input.equals("end")) {
                    System.out.println("聊天已终止，已与所有的聊天客户端终止连接");
                    Server.flagEnd=true;
                    Server.messages.clear();
                    synchronized (Server.links) {
                        for (Map.Entry<String, Link> entry : Server.links.entrySet()) {
                            entry.getValue().flagContinue = false;
                        }
                        Server.links.clear();
                    }
                    break; 
                }
                else 
                {
                    System.out.println("您输入的是无效字符,请重新输入");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                keyboardInput.close(); 
                System.exit(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
}
}

class Anteroom extends Thread {
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(12345);
            while (!Server.flagEnd) {
                    Socket clientSocket = serverSocket.accept();
                    Link link = new Link(clientSocket);
                    String nickname=link.receiver.clientToServer.readLine();
                    link.nickname=nickname;
                    synchronized (Server.links) {
                        Server.links.put(link.nickname, link);
                    }
                    Server.messages.add("["+ LocalDateTime.now().format(Server.formatter)+"]欢迎"+ link.nickname+"进入聊天室!");
                    Log.append("["+ LocalDateTime.now().format(Server.formatter)+"]欢迎"+ link.nickname+"进入聊天室!");
                
            }
        } catch (Exception e) {
            System.err.println("Anteroom 线程异常: " + e.getMessage());
        }
    }
}

class Link {
    Socket socket = null;
    String nickname = null; // 添加昵称字段
    boolean flagContinue = false;
    Receiver receiver = null;
    Sender sender = null;

    public Link(Socket socket) {
        flagContinue = true;
        this.socket = socket;
        receiver = new Receiver(this);
        sender = new Sender(this);
        receiver.start();
        sender.start();
    }

    class Receiver extends Thread {
        Link link = null;
        BufferedReader clientToServer = null;

        public Receiver(Link link) {
            this.link = link;
            try {
                clientToServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                System.err.println("初始化 BufferedReader 时出错: " + e.getMessage());
            }
        }

        public void run() {
            try {
                String receivedMessage;
                while (link.flagContinue && (receivedMessage = clientToServer.readLine()) != null) {
                    if (receivedMessage.equals(Client._endMark)) {
                        link.flagContinue = false;
                        synchronized (Server.links) {
                            Server.links.remove(link.nickname);
                        }
                        Server.messages.add("[" + LocalDateTime.now().format(Server.formatter) + "]" + link.nickname + " 退出了聊天室");
                        Log.append("[" + LocalDateTime.now().format(Server.formatter) + "]" + link.nickname + " 退出了聊天室");
                        break;
                    } else {
                        String message = "[" + link.nickname+":"+LocalDateTime.now().format(Server.formatter) + "] " + receivedMessage;
                        Server.messages.add(message);
                        Log.append(message);
                    }
                }
            } catch (IOException e) {
                System.err.println("接收消息异常: " + e.getMessage());
            }
        }
    }

    class Sender extends Thread {
        Link link = null;
        PrintWriter serverToclient = null;
        ConcurrentLinkedDeque<String> messagesToSend = new ConcurrentLinkedDeque<>();

        public Sender(Link link) {
            this.link = link;
            try {
                 serverToclient = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                System.err.println("初始化 PrintWriter 时出错: " + e.getMessage());
            }
        }

        public void run() {
                while (link.flagContinue) {
                    String message = messagesToSend.pollFirst();
                    if (message != null) {
                         serverToclient.println(message);
                    }
                }
   
        }
    }
}

class Log {
    static FileWriter writer = null;

    public static boolean createWriter(String filePath) {
        try {
            writer = new FileWriter(filePath, true);
            return true;
        } catch (IOException e) {
            System.err.println("日志文件流创建失败: " + e.getMessage());
            return false;
        }
    }

    public static boolean closeWriter() {
        try {
                writer.close();  
            return true;
        } catch (IOException e) {
            System.err.println("日志文件流关闭失败: " + e.getMessage());
            return false;
        }
    }

    public static boolean append(String content) {
        try {
                writer.write(content + "\n");
                writer.flush();
            return true;
        } catch (IOException e) {
            System.err.println("日志文件写入失败: " + e.getMessage());
            return false;
        }
    }
}
