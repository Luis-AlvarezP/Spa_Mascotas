package com.spamascotas.spa_mascotas_api.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.url}")
    private String appUrl;

    public void enviarActivacion(String destinatario, String token) {
        String link = appUrl + "/auth/activar?token=" + token;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(destinatario);
            message.setSubject("Activa tu cuenta — SpaMascotas");
            message.setText("Hola,\n\nActiva tu cuenta haciendo clic en el siguiente enlace "
                    + "(válido por 15 minutos):\n\n" + link
                    + "\n\nSi no solicitaste esto, ignora este mensaje.\n\n— SpaMascotas");
            mailSender.send(message);
            log.info("Email de activación enviado a {}", destinatario);
        } catch (Exception e) {
            log.warn("No se pudo enviar email a {}. Link de activación: {}", destinatario, link);
        }
    }

    public void enviarBienvenidaStaff(String destinatario, String nombre, String rol, String password) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(destinatario);
            message.setSubject("Bienvenido al equipo — SpaMascotas");
            message.setText("Hola " + nombre + ",\n\n"
                    + "Has sido registrado en el sistema SpaMascotas con el rol de " + rol + ".\n\n"
                    + "Tus credenciales de acceso:\n"
                    + "  Correo:     " + destinatario + "\n"
                    + "  Contraseña: " + password + "\n\n"
                    + "Accede en: " + appUrl + "\n"
                    + "Te recomendamos cambiar tu contraseña en el primer inicio de sesión.\n\n"
                    + "— SpaMascotas");
            mailSender.send(message);
            log.info("Email de bienvenida enviado a {} ({})", destinatario, rol);
        } catch (Exception e) {
            log.warn("No se pudo enviar email de bienvenida a {}: {}", destinatario, e.getMessage());
        }
    }

    public void enviarRestablecimiento(String destinatario, String token) {
        String link = appUrl + "/auth/reset-password?token=" + token;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(destinatario);
            message.setSubject("Restablece tu contraseña — SpaMascotas");
            message.setText("Hola,\n\nRestablece tu contraseña haciendo clic aquí "
                    + "(válido por 15 minutos):\n\n" + link
                    + "\n\nSi no solicitaste esto, ignora este mensaje.\n\n— SpaMascotas");
            mailSender.send(message);
            log.info("Email de restablecimiento enviado a {}", destinatario);
        } catch (Exception e) {
            log.warn("No se pudo enviar email a {}. Link de restablecimiento: {}", destinatario, link);
        }
    }
}
