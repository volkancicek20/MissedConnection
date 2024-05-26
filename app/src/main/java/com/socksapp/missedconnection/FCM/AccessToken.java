package com.socksapp.missedconnection.FCM;

import android.util.Log;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.collect.Lists;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class AccessToken {
    private static final String firebaseMessagingScape = "https://www.googleapis.com/auth/firebase.messaging";

    public String getAccessToken(){
        try {
            String jsonString = "{\n" +
                    "  \"type\": \"service_account\",\n" +
                    "  \"project_id\": \"missedconnection-c000f\",\n" +
                    "  \"private_key_id\": \"289ebd8bf2c3a27a08139ae48827d7ac563c9e89\",\n" +
                    "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDETcuSy5+y/R7g\\nly1ZQnufBYSv0QSK91NvPwaAV2MyK1y740oTQyxcDL8Xs1JcmCFZ9kTIO0XLkpNJ\\nrwu6ZPe1hOU7rrq/9IaHLiTfUDmo4jVWWsaQ/arcYePdYu3UBHgI3ZIfFCypLbVS\\nkBEUXA4lOBICszyl/jxwZpyC4Ytjf05bXJ6vr3meJuHHsFmZAvC/i0ltKF204ZDE\\nnYJoOz6T+p6Wj1kezWcft9vbdWoWjQ0IIIERov/4oF/+h+KlDbuiIQtcfdW+NeWP\\nhAd80Fm7Y5HQXSZY7lBNVf635eoHA66yZlzklEVSTP/AfjehHIExel/mofANmtq9\\n+b5TDx1pAgMBAAECggEAAuyvSJy2wH+HpRwveWXVimcWRNnJ9emNIUpItiMHJ+1Q\\nNH7kgMC2kZkBhFCAPL+p+ImEGRET/z5L6jQ8yh48uAQ2t5LI9rjlg7BO7ZJIB6F2\\nHrcbv/d9gwrEzqytgyP9Z1FovsSJzUvwaH5buMhN0CVjlE14DPeKjjijIoqKbXUm\\ngf8sd2fsPswEpvJ3Wkw/+CYiBnKdi7KFS3ZXWvgBpyYHJjTt8g/dZxnHMU+XTnb9\\nnrUuM9teYUP3GdO9eHcq/tfHg8zmVR6YwhXbzy6APRwqjey3gSCGeLaaFyyVi3wM\\nE0YcCag1JjU91npQdwiobpz5yhI+NShDpKOYdQyVgQKBgQDynjGr43/IvAAJoRz+\\nt1HLElK28oFVjhXALW+epQimCCnQT2EVK1tV6kXxiTCn2yMeED3NQIjYDDwMkZrB\\nrrg7VqLAl0jSWps5UAmw0taRFhEMvIOp2kZGZnEMLDHL6nASS3Sz2yhwX+sblc2+\\ndUZq2zEPqppD8ST+aQND1FnGgQKBgQDPIaORRfBlm9jroLHBqHlT+wAW1VW4dfYg\\n5Fwd0qNGVyH5pHfvVHlzYHaGb0w7tjTqS4uL2upwpaw8JQdFKB1WFCnvgXwqBmFA\\nZ9NAAaGThbhW14oPXVExmDd95SoUlszzRKV6CLvzQJxIdtEhq8FAcky/XBuGpL5H\\nVSEw4PRy6QKBgBWSUTJKSPtNE+fHRm+zVvdqSqZvi2ZS+sYahBkj7U/t2+HknOon\\n77gE23EtVIEoL8glg4kzWrdy5wfrDkp4QXMtMc1T5iql2bVP03zAUkmWH6/1NvOs\\nL6FkzFpbt5W843gM83RmYQMU1C+gsyli/f7UPFl7ProZv8NLDPpD8DOBAoGBAJUG\\nbtimqT9x0bA1/a6HuENj47yJ4RLGTD9+DYindDG3nwot+tyGupr4XE585SxQ++FW\\nWSBWcTZ6/GCJg6GOKw0zlLhtQRg7Xt0n1iXHBqlNHEPe78X+Ldyw17wMlSobFXox\\neZ1Di3aIlejNE8pV+MSVeAJ2EpfdD8nixe6U60uBAoGBAITWzEcjIawIrfo2w4qs\\njo33mxjIEsAGarNEp/YlKis4LdQjWZZpp1JP6B24+vPN2WOEeQIXzACVCMR5Rm1z\\neHYBJ6EDpacM0PdpBOuJh/czcs9RlebtEZeJisOE3EwzblhllKMHdv//fzyikUjz\\nyOGxmoVrrFN2ZLjvCuKd1Dcl\\n-----END PRIVATE KEY-----\\n\",\n" +
                    "  \"client_email\": \"firebase-adminsdk-rok6k@missedconnection-c000f.iam.gserviceaccount.com\",\n" +
                    "  \"client_id\": \"114558820481880724085\",\n" +
                    "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
                    "  \"token_uri\": \"https://oauth2.googleapis.com/token\",\n" +
                    "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n" +
                    "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-rok6k%40missedconnection-c000f.iam.gserviceaccount.com\",\n" +
                    "  \"universe_domain\": \"googleapis.com\"\n" +
                    "}\n";

            InputStream stream = new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8));

            GoogleCredentials googleCredentials = GoogleCredentials.fromStream(stream).createScoped(Lists.newArrayList(firebaseMessagingScape));

            googleCredentials.refresh();

            return googleCredentials.getAccessToken().getTokenValue();
        }catch (Exception e){
            Log.e("error", " " + e.getMessage());
            return null;
        }
    }
}
