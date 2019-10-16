public class StaticResource {
    public static String basePath = "/home/cary/Documents/Data/Apk/";
    public static String token = "3ebfb3b586b9551c97275d6cae9cd5c8bcc9eb93";
    public static String getToken(int id) {

        id = id % 6;
        String token;
        switch (id) {
            case 0:
                token = "a0611d83dffae80fb4a5fd4da5ae0dc03315dce3";
                break;
            case 1:
                token = "9f3f63328912ee07ba6dbf752f2523db1f83377a";
                break;
            case 2:
                token = "d61646bc6ee1adad35d720485a41ec81b97d1fc1";
                break;
            case 3:
                token = "49856646e12d728917fd05a59ed13496b3e43009";
                break;
            case 4:
                token = "bd5c15538ec9619b4013b8e0372414860ab803a0";
                break;
            default:
                token = "bb6b03aa1bedb7080319d8244f302e02eaf35a72";
                break;
        }
        return token;
    }
}