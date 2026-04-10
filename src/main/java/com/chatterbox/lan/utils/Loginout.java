package com.chatterbox.lan.utils;
import org.mindrot.jbcrypt.BCrypt;

public class Loginout {
    public static String Hasher(String password){
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
    public static boolean ValidatePass(String password, String hashedPassword){
        return BCrypt.checkpw(password, hashedPassword);
    }
}
