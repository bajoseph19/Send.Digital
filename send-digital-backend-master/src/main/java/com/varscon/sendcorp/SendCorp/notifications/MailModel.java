package com.varscon.sendcorp.SendCorp.notifications;

import lombok.*;

import java.util.List;
import java.util.Map;


@EqualsAndHashCode()
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MailModel {
    private String from;
    private String mailTo;
    private String subject;
    private String template;
    private List<Object> attachments;
    private Map<String, Object> props;
}
