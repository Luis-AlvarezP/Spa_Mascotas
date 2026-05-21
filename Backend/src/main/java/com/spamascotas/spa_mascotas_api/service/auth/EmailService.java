package com.spamascotas.spa_mascotas_api.service.auth;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.url}")
    private String appUrl;

    // ── Auth ──────────────────────────────────────────────────

    public void enviarActivacion(String destinatario, String token) {
        String link = appUrl + "/auth/activar?token=" + token;
        enviarHtml(destinatario,
                "Activa tu cuenta — SpaMascotas",
                plantilla("Activa tu cuenta 🐾", null,
                        "Gracias por registrarte en <strong style='color:#c4b5fd'>SpaMascotas</strong>. "
                        + "Para completar tu registro haz clic en el botón de abajo.",
                        new String[][]{
                            {"Válido por", "15 minutos"},
                            {"Correo",     destinatario}
                        },
                        link, "Activar mi cuenta",
                        "Si no creaste una cuenta en SpaMascotas, puedes ignorar este mensaje de forma segura.",
                        "#7c3aed"));
    }

    public void enviarRestablecimiento(String destinatario, String token) {
        String link = appUrl + "/auth/reset-password?token=" + token;
        enviarHtml(destinatario,
                "Restablece tu contraseña — SpaMascotas",
                plantilla("Restablecer contraseña 🔐", null,
                        "Recibimos una solicitud para restablecer la contraseña de tu cuenta. "
                        + "Haz clic en el botón para crear una nueva.",
                        new String[][]{
                            {"Válido por", "15 minutos"},
                            {"Correo",     destinatario}
                        },
                        link, "Restablecer contraseña",
                        "Si no solicitaste este cambio, ignora este mensaje. Tu contraseña actual sigue siendo la misma.",
                        "#ef4444"));
    }

    public void enviarBienvenidaStaff(String destinatario, String nombre, String rol, String password) {
        String rolLabel = switch (rol) {
            case "RECEPCION" -> "Recepcionista";
            case "GROOMER"   -> "Groomer";
            case "ADMIN"     -> "Administrador";
            default          -> rol;
        };
        enviarHtml(destinatario,
                "¡Bienvenido al equipo! — SpaMascotas",
                plantilla("¡Bienvenido al equipo! 🎉", nombre,
                        "Has sido registrado en el sistema de gestión de <strong style='color:#c4b5fd'>SpaMascotas</strong>. "
                        + "A continuación encontrarás tus credenciales de acceso.",
                        new String[][]{
                            {"Nombre",             nombre},
                            {"Rol",                rolLabel},
                            {"Correo de acceso",   destinatario},
                            {"Contraseña temporal", password}
                        },
                        appUrl + "/auth/login", "Ingresar al sistema",
                        "Por seguridad, te recomendamos cambiar tu contraseña en cuanto inicies sesión por primera vez. "
                        + "Dirígete a tu perfil para hacerlo.",
                        "#7c3aed"));
    }

    // ── Citas ─────────────────────────────────────────────────

    public void enviarCitaEnRevision(String destinatario, String nombreCliente,
                                      String servicio, String fecha) {
        enviarHtml(destinatario,
                "Tu cita está en revisión — SpaMascotas",
                plantilla("Solicitud recibida ✓", nombreCliente,
                        "Hemos recibido tu solicitud de cita. Nuestro equipo la está revisando y te notificará pronto.",
                        new String[][]{
                            {"Servicio", servicio},
                            {"Fecha",    fecha},
                            {"Estado",   "En revisión"}
                        },
                        null, null,
                        "Una vez confirmada recibirás otro correo con todos los detalles.",
                        "#a78bfa"));
    }

    public void enviarCitaConfirmada(String destinatario, String nombreCliente,
                                      String servicio, String fecha, String groomer) {
        enviarHtml(destinatario,
                "¡Cita confirmada! — SpaMascotas",
                plantilla("¡Cita confirmada! 🎉", nombreCliente,
                        "¡Buenas noticias! Tu cita ha sido aceptada y está agendada. Te esperamos.",
                        new String[][]{
                            {"Servicio", servicio},
                            {"Fecha",    fecha},
                            {"Groomer",  groomer != null ? groomer : "Por asignar"},
                            {"Estado",   "Confirmada"}
                        },
                        appUrl + "/agenda", "Ver mis citas",
                        "Recuerda cancelar o reprogramar con al menos 24 horas de anticipación para evitar cargos adicionales.",
                        "#22c55e"));
    }

    public void enviarCitaCancelada(String destinatario, String nombreCliente,
                                     String servicio, String fecha, String motivo) {
        enviarHtml(destinatario,
                "Cita cancelada — SpaMascotas",
                plantilla("Cita cancelada ✕", nombreCliente,
                        "Tu cita ha sido cancelada. Si tienes dudas no dudes en contactarnos.",
                        new String[][]{
                            {"Servicio", servicio},
                            {"Fecha",    fecha},
                            {"Motivo",   motivo != null && !motivo.isBlank() ? motivo : "—"}
                        },
                        appUrl + "/agenda", "Solicitar nueva cita",
                        "Puedes agendar una nueva cita cuando lo desees desde nuestra plataforma.",
                        "#ef4444"));
    }

    public void enviarCitaReprogramada(String destinatario, String nombreCliente,
                                        String servicio, String fechaNueva) {
        enviarHtml(destinatario,
                "Cita reprogramada — SpaMascotas",
                plantilla("Cita reprogramada 📅", nombreCliente,
                        "Tu cita ha sido reprogramada exitosamente y quedó en revisión nuevamente.",
                        new String[][]{
                            {"Servicio",    servicio},
                            {"Nueva fecha", fechaNueva},
                            {"Estado",      "En revisión"}
                        },
                        null, null,
                        "Te notificaremos una vez que sea confirmada por nuestro equipo.",
                        "#a78bfa"));
    }

    public void enviarListaParaRecoger(String destinatario, String nombreCliente,
                                       String mascota, String servicio, String groomer) {
        enviarHtml(destinatario,
                "¡" + mascota + " está lista para recoger! — SpaMascotas",
                plantilla("¡Lista para recoger! 🐾", nombreCliente,
                        "<strong style='color:#c4b5fd'>" + mascota + "</strong> ya terminó su sesión de grooming "
                        + "y te está esperando en el spa. ¡Pasa cuando puedas!",
                        new String[][]{
                            {"Mascota",  mascota},
                            {"Servicio", servicio},
                            {"Groomer",  groomer}
                        },
                        null, null,
                        "El cobro del servicio se registrará al momento del retiro en recepción.",
                        "#34d399"));
    }

    public void enviarRecordatorio(String destinatario, String nombreCliente,
                                    String servicio, String fecha, int horas) {
        boolean es24 = horas == 24;
        enviarHtml(destinatario,
                (es24 ? "Recordatorio: tu cita es mañana" : "Tu cita es en 2 horas") + " — SpaMascotas",
                plantilla(es24 ? "Tu cita es mañana 🐾" : "Tu cita es en 2 horas 🕐", nombreCliente,
                        es24 ? "Te recordamos que mañana tienes una cita en SpaMascotas. ¡Te esperamos!"
                             : "¡Tu cita comienza en 2 horas! Prepárate para traer a tu mascota.",
                        new String[][]{
                            {"Servicio", servicio},
                            {"Fecha",    fecha},
                            {"Tiempo",   es24 ? "Mañana" : "En 2 horas"}
                        },
                        appUrl + "/agenda", "Ver mis citas",
                        es24 ? "Si necesitas cancelar o reprogramar, hazlo lo antes posible para evitar cargos."
                             : "Si tienes algún imprevisto de último momento, contáctanos directamente.",
                        "#fbbf24"));
    }

    public void enviarReciboCita(String destinatario, String nombreCliente, String ci,
                                  String mascota, String servicio, String groomer, String duracion,
                                  String fecha, String metodoPago,
                                  String precioBase, String recargoPorcentaje, String totalFinal,
                                  Long citaId) {
        java.util.List<String[]> filas = new java.util.ArrayList<>();
        filas.add(new String[]{"N° Recibo",      "#" + citaId});
        if (ci != null && !ci.isBlank()) {
            filas.add(new String[]{"CI Cliente",     ci});
        }
        filas.add(new String[]{"Mascota",        mascota});
        filas.add(new String[]{"Servicio",       servicio});
        filas.add(new String[]{"Duración",       duracion + " min"});
        filas.add(new String[]{"Groomer",        groomer});
        filas.add(new String[]{"Fecha",          fecha});
        filas.add(new String[]{"Método de pago", metodoPago});
        if (recargoPorcentaje != null && !recargoPorcentaje.equals("0")) {
            filas.add(new String[]{"Precio base",    "Bs. " + precioBase});
            filas.add(new String[]{"Recargo",        "+" + recargoPorcentaje + "%"});
        }
        filas.add(new String[]{"Total pagado",   "Bs. " + totalFinal});
        enviarHtml(destinatario,
                "Recibo de servicio #" + citaId + " — SpaMascotas",
                plantilla("Pago registrado ✔", nombreCliente,
                        "El pago de tu servicio ha sido registrado exitosamente. Gracias por confiar en nosotros.",
                        filas.toArray(new String[0][]),
                        null, null,
                        "Guarda este correo como comprobante de pago. ¡Esperamos verte pronto en SpaMascotas!",
                        "#22c55e"));
    }

    // ── Infraestructura ───────────────────────────────────────

    private void enviarHtml(String destinatario, String asunto, String html) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(html, true);
            mailSender.send(msg);
            log.info("Email '{}' enviado a {}", asunto, destinatario);
        } catch (MessagingException e) {
            log.warn("No se pudo enviar email '{}' a {}: {}", asunto, destinatario, e.getMessage());
        }
    }

    private String plantilla(String titulo, String nombre, String intro,
                              String[][] filas, String ctaLink, String ctaTexto,
                              String pie, String accentColor) {
        StringBuilder sb = new StringBuilder();

        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='margin:0;padding:20px;background:#080612;font-family:Arial,Helvetica,sans-serif'>");
        sb.append("<div style='max-width:540px;margin:0 auto'>");

        // Header
        sb.append("<div style='background:linear-gradient(135deg,#1e0a3c,#2d1060);border-radius:14px 14px 0 0;padding:28px 32px;border-bottom:3px solid ").append(accentColor).append("'>");
        sb.append("<p style='margin:0;font-size:12px;color:#a78bfa;letter-spacing:0.1em;text-transform:uppercase;font-weight:600'>SpaMascotas</p>");
        sb.append("<h1 style='margin:6px 0 0;color:#ffffff;font-size:22px;font-weight:800'>").append(titulo).append("</h1>");
        sb.append("</div>");

        // Body
        sb.append("<div style='background:#0f0b1e;padding:28px 32px;border-left:1px solid #1e1a30;border-right:1px solid #1e1a30'>");

        if (nombre != null && !nombre.isBlank()) {
            sb.append("<p style='margin:0 0 16px;color:#94a3b8;font-size:15px'>Hola, <strong style='color:#e2e8f0'>").append(nombre).append("</strong> 👋</p>");
        }
        sb.append("<p style='margin:0 0 24px;color:#94a3b8;font-size:14px;line-height:1.6'>").append(intro).append("</p>");

        // Tabla de datos
        if (filas != null && filas.length > 0) {
            sb.append("<table style='width:100%;border-collapse:separate;border-spacing:0 4px;margin-bottom:24px'>");
            for (String[] fila : filas) {
                sb.append("<tr>");
                sb.append("<td style='padding:10px 14px;background:#1a1530;border-radius:6px 0 0 6px;color:#6b7280;font-size:12px;font-weight:600;text-transform:uppercase;letter-spacing:0.05em;width:38%;border-left:3px solid ").append(accentColor).append("22'>").append(fila[0]).append("</td>");
                sb.append("<td style='padding:10px 14px;background:#1a1530;border-radius:0 6px 6px 0;color:#e2e8f0;font-size:13px;font-weight:600'>").append(fila[1]).append("</td>");
                sb.append("</tr>");
            }
            sb.append("</table>");
        }

        // Botón CTA
        if (ctaLink != null && ctaTexto != null) {
            sb.append("<div style='text-align:center;margin-bottom:24px'>");
            sb.append("<a href='").append(ctaLink).append("' style='display:inline-block;padding:13px 32px;background:").append(accentColor).append(";color:#ffffff;text-decoration:none;border-radius:8px;font-size:14px;font-weight:700;letter-spacing:0.02em'>")
              .append(ctaTexto).append(" →</a>");
            sb.append("</div>");
        }

        sb.append("</div>");

        // Footer
        sb.append("<div style='background:#0a0818;border-radius:0 0 14px 14px;padding:18px 32px;border:1px solid #1e1a30;border-top:none'>");
        sb.append("<p style='margin:0 0 6px;font-size:12px;color:#475569;line-height:1.5'>").append(pie).append("</p>");
        sb.append("<p style='margin:0;font-size:11px;color:#334155'>© SpaMascotas · <a href='").append(appUrl).append("' style='color:#7c3aed;text-decoration:none'>").append(appUrl).append("</a></p>");
        sb.append("</div>");

        sb.append("</div></body></html>");
        return sb.toString();
    }
}
