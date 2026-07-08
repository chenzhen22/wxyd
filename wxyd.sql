CREATE DATABASE `wxyd`;

USE `wxyd`;

CREATE TABLE IF NOT EXISTS `note`
(
  `id`     int(11) unsigned NOT NULL AUTO_INCREMENT,
  `ip`     varchar(64)      NOT NULL,
  `string` varchar(6400)    NOT NULL,
  `time`   timestamp        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `operinfo`
(
  `id`   int(11) unsigned NOT NULL AUTO_INCREMENT,
  `ip`   varchar(64)      NOT NULL,
  `info` varchar(256),
  `time` timestamp        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8;
ALTER TABLE operinfo
  ADD INDEX operinfo_index (ip);

CREATE TABLE IF NOT EXISTS `message`
(
  `id`   int(11) unsigned NOT NULL AUTO_INCREMENT,
  `ip`   varchar(64)      NOT NULL,
  `info` varchar(614),
  `time` timestamp        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8;
ALTER TABLE message
  ADD INDEX message_index (ip);

CREATE TABLE IF NOT EXISTS `client`
(
  `ip`       varchar(64)  NOT NULL,
  `status`   char(1)      NOT NULL,
  `username` varchar(614) NOT NULL,
  PRIMARY KEY (`ip`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `action`
(
  `id`      int(11) unsigned NOT NULL AUTO_INCREMENT,
  `actions` varchar(40)      NOT NULL,
  PRIMARY KEY (`id`, `actions`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8;

INSERT INTO action (id, actions) VALUES (1, 'queryMsgCode');
INSERT INTO action (id, actions) VALUES (2, 'uploadFileToBank');
INSERT INTO action (id, actions) VALUES (4, 'message');
INSERT INTO action (id, actions) VALUES (5, 'queryMessage');
INSERT INTO action (id, actions) VALUES (6, 'queryUdInfo');
INSERT INTO action (id, actions) VALUES (7, 'start');
INSERT INTO action (id, actions) VALUES (8, 'orderCreate');
INSERT INTO action (id, actions) VALUES (9, 'error');
INSERT INTO action (id, actions) VALUES (10, 'queryMsgCode');
INSERT INTO action (id, actions) VALUES (11, 'uploadFileToBank');
INSERT INTO action (id, actions) VALUES (13, 'message');
INSERT INTO action (id, actions) VALUES (14, 'queryMessage');
INSERT INTO action (id, actions) VALUES (15, 'queryUdInfo');
INSERT INTO action (id, actions) VALUES (16, 'addWhite');
INSERT INTO action (id, actions) VALUES (17, 'queryWhiteInfo');
INSERT INTO action (id, actions) VALUES (18, 'updateWhite');
INSERT INTO action (id, actions) VALUES (19, 'deleteWhite');
INSERT INTO action (id, actions) VALUES (20, 'queryUdOper');
INSERT INTO action (id, actions) VALUES (21, 'queryMsgCode');
INSERT INTO action (id, actions) VALUES (22, 'uploadFileToBank');
INSERT INTO action (id, actions) VALUES (23, 'mmLogin');
INSERT INTO action (id, actions) VALUES (24, 'addMessage');
INSERT INTO action (id, actions) VALUES (25, 'queryMessage');
INSERT INTO action (id, actions) VALUES (26, 'queryUdInfo');
INSERT INTO action (id, actions) VALUES (27, 'start');
INSERT INTO action (id, actions) VALUES (28, 'orderCreate');
INSERT INTO action (id, actions) VALUES (29, 'error');
INSERT INTO action (id, actions) VALUES (30, 'addWhite');
INSERT INTO action (id, actions) VALUES (31, 'queryWhiteInfo');
INSERT INTO action (id, actions) VALUES (32, 'updateWhite');
INSERT INTO action (id, actions) VALUES (33, 'deleteWhite');
INSERT INTO action (id, actions) VALUES (34, 'queryUdOper');
INSERT INTO action (id, actions) VALUES (35, 'docQry');
INSERT INTO action (id, actions) VALUES (36, 'delMessage');
INSERT INTO action (id, actions) VALUES (37, 'uploadDocumentFile');
INSERT INTO action (id, actions) VALUES (38, 'queryDocumentFileList');
INSERT INTO action (id, actions) VALUES (39, 'deleteDocumentFile');

CREATE TABLE IF NOT EXISTS `pwmanagement`
(
  `id`       int(11) unsigned NOT NULL AUTO_INCREMENT,
  `type`     varchar(20)      NOT NULL,
  `password` varchar(20)      NOT NULL,
  `desc`     varchar(20),
  PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8;

INSERT INTO `pwmanagement` (`type`, `password`, `desc`)
VALUES ('restart', '~1q2w3e4r', '重启应用密码');

CREATE TABLE IF NOT EXISTS `socketMessage`
(
  `date`     varchar(20),
  `clientIp` varchar(20),
  `serverIp` varchar(20),
  `type`     char(1) NOT NULL,
  `url`      varchar(614),
  `message`  varchar(6000),
  `recive`   varchar(6000),
  `status`   char(1),
  PRIMARY KEY (`date`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8;
ALTER TABLE socketMessage
  ADD INDEX socketMessage_index1 (`type`);
ALTER TABLE socketMessage
  ADD INDEX socketMessage_index2 (`status`);
ALTER TABLE socketMessage
  ADD INDEX socketMessage_index3 (`clientIp`);

CREATE TABLE IF NOT EXISTS `requestlog`
(
  `clinetIp` varchar(20) NOT NULL,
  `url`      varchar(200) NOT NULL,
  `data`     varchar(6000),
  `now`      varchar(20)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `doc`
(
  `transcode` varchar(60),
  `transname` varchar(80),
  `project`   varchar(60),
  `blname`    varchar(100),
  `type`      varchar(10),
  PRIMARY KEY (`transcode`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `docFile`
(
  `fileName` varchar(60),
  `type`     varchar(10),
  `time`     varchar(60),
  PRIMARY KEY (`fileName`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `documentFile`
(
  `fileUUID`   varchar(60),
  `clientIp`    varchar(30),
  `userName`   varchar(80),
  `fileName`   varchar(200),
  `createTime` varchar(20),
  PRIMARY KEY (`fileUUID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;