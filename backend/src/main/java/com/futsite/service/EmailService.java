package com.futsite.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("futsite@example.com");
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            // Log but don't fail — email is non-critical
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    public void notifyAthleteAddedToTeam(String email, String athleteName, String teamName) {
        String subject = "FutSite - Você foi adicionado ao time " + teamName;
        String body = String.format(
                "Olá %s,\n\nVocê foi adicionado ao time '%s' no FutSite.\n\nBom jogo!\nEquipe FutSite",
                athleteName, teamName);
        sendEmail(email, subject, body);
    }

    public void notifyCaptainTeamRegistered(String email, String captainName, String teamName, String championshipName) {
        String subject = "FutSite - Time inscrito no campeonato " + championshipName;
        String body = String.format(
                "Olá %s,\n\nSeu time '%s' foi inscrito no campeonato '%s'.\n\nBoa sorte!\nEquipe FutSite",
                captainName, teamName, championshipName);
        sendEmail(email, subject, body);
    }

    public void notifyChampionshipStarted(String email, String athleteName, String championshipName) {
        String subject = "FutSite - Campeonato iniciado: " + championshipName;
        String body = String.format(
                "Olá %s,\n\nO campeonato '%s' foi iniciado!\n\nPrepare-se para os jogos!\nEquipe FutSite",
                athleteName, championshipName);
        sendEmail(email, subject, body);
    }
}
