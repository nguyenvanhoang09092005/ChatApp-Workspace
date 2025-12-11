-- --------------------------------------------------------
-- Host:                         
-- Server version:               8.4.4 - MySQL Community Server - GPL
-- Server OS:                    Win64
-- HeidiSQL Version:             12.7.0.6850
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

-- Dumping structure for table chatapp.contacts
CREATE TABLE IF NOT EXISTS `contacts` (
  `contact_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `contact_user_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'pending',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`contact_id`),
  KEY `user_id` (`user_id`),
  KEY `contact_user_id` (`contact_user_id`),
  CONSTRAINT `contacts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `contacts_ibfk_2` FOREIGN KEY (`contact_user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table chatapp.contacts: ~0 rows (approximately)

-- Dumping structure for table chatapp.conversations
CREATE TABLE IF NOT EXISTS `conversations` (
  `conversation_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'private',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avatar_url` text COLLATE utf8mb4_unicode_ci,
  `description` text COLLATE utf8mb4_unicode_ci,
  `member_ids` text COLLATE utf8mb4_unicode_ci,
  `creator_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_message` text COLLATE utf8mb4_unicode_ci,
  `last_message_time` datetime DEFAULT NULL,
  `unread_count` int DEFAULT '0',
  `is_active` tinyint(1) DEFAULT '1',
  `is_muted` tinyint(1) DEFAULT '0',
  `is_pinned` tinyint(1) DEFAULT '0',
  `is_archived` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`conversation_id`),
  KEY `creator_id` (`creator_id`),
  CONSTRAINT `conversations_ibfk_1` FOREIGN KEY (`creator_id`) REFERENCES `users` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table chatapp.conversations: ~3 rows (approximately)
INSERT INTO `conversations` (`conversation_id`, `type`, `name`, `avatar_url`, `description`, `member_ids`, `creator_id`, `created_at`, `updated_at`, `last_message`, `last_message_time`, `unread_count`, `is_active`, `is_muted`, `is_pinned`, `is_archived`) VALUES
	('171ed8c5-4bef-4350-955b-b3409d6c4e51', 'private', NULL, NULL, NULL, '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01,303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', '2025-12-03 02:35:09', '2025-12-06 03:43:04', '', '2025-12-06 03:43:04', 0, 1, 0, 0, 0),
	('7683c268-bed6-4429-b1ad-0cfc98ec495d', 'private', NULL, NULL, NULL, '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c,6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', '2025-12-02 14:44:34', '2025-12-06 07:39:45', 'Sticker', '2025-12-06 07:39:45', 0, 1, 0, 0, 0),
	('b8249cfb-1892-4691-ad72-d3f99ac21bd7', 'private', NULL, NULL, NULL, '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c,303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', '2025-12-02 14:39:33', '2025-12-06 04:31:24', '', '2025-12-06 04:31:24', 0, 1, 0, 0, 0);

-- Dumping structure for table chatapp.conversation_members
CREATE TABLE IF NOT EXISTS `conversation_members` (
  `conversation_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `joined_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`conversation_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `conversation_members_ibfk_1` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`conversation_id`) ON DELETE CASCADE,
  CONSTRAINT `conversation_members_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table chatapp.conversation_members: ~6 rows (approximately)

-- Dumping structure for procedure chatapp.GetConversationMessages
DELIMITER //
CREATE PROCEDURE `GetConversationMessages`(
    IN convId VARCHAR(36),
    IN offsetVal INT,
    IN limitVal INT
)
BEGIN
    SELECT 
        m.message_id,
        m.conversation_id,
        m.sender_id,
        u.display_name as sender_name,
        u.avatar_url as sender_avatar,
        m.content,
        m.message_type,
        m.media_url,
        m.file_name,
        m.file_size,
        m.is_edited,
        m.is_recalled,
        m.created_at,
        GROUP_CONCAT(DISTINCT mr.user_id) as read_by,
        GROUP_CONCAT(DISTINCT CONCAT(mre.user_id, ':', mre.emoji)) as reactions
    FROM messages m
    INNER JOIN users u ON m.sender_id = u.user_id
    LEFT JOIN message_reads mr ON m.message_id = mr.message_id
    LEFT JOIN message_reactions mre ON m.message_id = mre.message_id
    WHERE m.conversation_id = convId
    GROUP BY m.message_id
    ORDER BY m.created_at DESC
    LIMIT limitVal OFFSET offsetVal;
END//
DELIMITER ;

-- Dumping structure for procedure chatapp.GetUserConversations
DELIMITER //
CREATE PROCEDURE `GetUserConversations`(IN userId VARCHAR(36))
BEGIN
    SELECT 
        c.conversation_id,
        c.type,
        c.name,
        c.avatar_url,
        cm.unread_count,
        cm.is_pinned,
        cm.is_muted,
        m.content as last_message,
        m.created_at as last_message_time,
        c.updated_at
    FROM conversations c
    INNER JOIN conversation_members cm ON c.conversation_id = cm.conversation_id
    LEFT JOIN (
        SELECT conversation_id, content, created_at,
               ROW_NUMBER() OVER (PARTITION BY conversation_id ORDER BY created_at DESC) as rn
        FROM messages
    ) m ON c.conversation_id = m.conversation_id AND m.rn = 1
    WHERE cm.user_id = userId AND c.is_active = TRUE
    ORDER BY cm.is_pinned DESC, c.updated_at DESC;
END//
DELIMITER ;

-- Dumping structure for table chatapp.messages
CREATE TABLE IF NOT EXISTS `messages` (
  `message_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `conversation_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sender_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sender_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sender_avatar` text COLLATE utf8mb4_unicode_ci,
  `content` text COLLATE utf8mb4_unicode_ci,
  `message_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'TEXT',
  `media_url` text COLLATE utf8mb4_unicode_ci,
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_size` bigint DEFAULT NULL,
  `thumbnail_url` text COLLATE utf8mb4_unicode_ci,
  `timestamp` datetime DEFAULT CURRENT_TIMESTAMP,
  `media_duration` int DEFAULT NULL,
  `is_read` tinyint(1) DEFAULT '0',
  `is_delivered` tinyint(1) DEFAULT '0',
  `is_edited` tinyint(1) DEFAULT '0',
  `is_recalled` tinyint(1) DEFAULT '0',
  `reply_to_message_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`message_id`),
  KEY `conversation_id` (`conversation_id`),
  KEY `sender_id` (`sender_id`),
  KEY `reply_to_message_id` (`reply_to_message_id`),
  CONSTRAINT `messages_ibfk_1` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`conversation_id`) ON DELETE CASCADE,
  CONSTRAINT `messages_ibfk_2` FOREIGN KEY (`sender_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `messages_ibfk_3` FOREIGN KEY (`reply_to_message_id`) REFERENCES `messages` (`message_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table chatapp.messages: ~136 rows (approximately)
INSERT INTO `messages` (`message_id`, `conversation_id`, `sender_id`, `sender_name`, `sender_avatar`, `content`, `message_type`, `media_url`, `file_name`, `file_size`, `thumbnail_url`, `timestamp`, `media_duration`, `is_read`, `is_delivered`, `is_edited`, `is_recalled`, `reply_to_message_id`) VALUES
	('001d8010-7276-4dfd-abfb-0fad69312ba7', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'huuu', 'text', NULL, NULL, 0, NULL, '2025-12-04 12:15:08', 0, 1, 1, 0, 0, NULL),
	('014e2412-d63c-4324-82b5-2bac4f6e3c5b', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764929040/chat_images/chat_images/f80dfa6e-f493-41b6-bd64-7030417aa2b1.png', 'Screenshot 2025-11-19 083217.png', 145740, NULL, '2025-12-05 10:04:00', 0, 1, 1, 0, 0, NULL),
	('04b64375-0f38-429d-9a79-a6bfa3608ed0', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'a', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:46:25', 0, 1, 1, 0, 0, NULL),
	('04e0feb6-57a4-412f-88a5-e8adea93bdf9', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'a', 'text', NULL, NULL, 0, NULL, '2025-12-06 03:34:12', 0, 1, 1, 0, 0, NULL),
	('05bf7db6-c5ff-4c8d-a01b-7885f86b7699', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, 'alo', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:58:44', 0, 1, 1, 0, 0, NULL),
	('06c9e3d3-e621-467f-8a53-710e227ba329', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'xin chòa', 'text', NULL, NULL, 0, NULL, '2025-12-04 12:09:26', 0, 1, 1, 0, 0, NULL),
	('0e9d8d0a-e663-4f84-b9cf-fe26d21cf352', '171ed8c5-4bef-4350-955b-b3409d6c4e51', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, '?', 'text', NULL, NULL, 0, NULL, '2025-12-06 02:37:20', 0, 1, 1, 0, 0, NULL),
	('134e323f-2b8f-408b-adc6-264618d7d77e', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, '🤗', 'TEXT', NULL, NULL, 0, NULL, '2025-12-06 07:39:00', 0, 1, 1, 0, 0, NULL),
	('14747bea-6646-4ad5-896f-ad06af518857', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, 'hahahahha', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:55:08', 0, 1, 1, 0, 0, NULL),
	('1616a0f4-570b-477c-8b6c-448405e2f88c', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'hu', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:15:54', 0, 1, 1, 0, 0, NULL),
	('183bee06-b097-49d0-8452-bec775f00e58', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764992900/chat_images/chat_images/36c0db11-4494-4bf2-9782-23b821ff8427.jpg', '43-1.jpg', 1553278, NULL, '2025-12-06 03:48:20', 0, 1, 1, 0, 0, NULL),
	('19f74124-9e7d-4da8-8319-d83fce3a02b5', '171ed8c5-4bef-4350-955b-b3409d6c4e51', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'aaa', 'text', NULL, NULL, 0, NULL, '2025-12-06 02:37:25', 0, 1, 1, 0, 0, NULL),
	('1a0133d2-81a7-41bc-a03f-909dc811bf1d', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764994269/chat_images/chat_images/86c21a80-12d0-40b3-a380-cebd65eec93c.jpg', 'cầu lông.jpg', 10213, NULL, '2025-12-06 04:11:08', 0, 1, 1, 0, 0, NULL),
	('1b16a49c-75d2-4d2b-a0b1-64bfb12cc278', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'a', 'text', NULL, NULL, 0, NULL, '2025-12-06 03:34:52', 0, 1, 1, 0, 0, NULL),
	('205d0d2c-a3b3-476c-8f23-1964eb65347a', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, '', 'STICKER', 'https://example.com/stickers/memes/02.png', 'This is Fine', 0, NULL, '2025-12-06 07:39:45', 0, 0, 0, 0, 0, NULL),
	('206678fb-ace0-40ce-8e0f-e9d4712a84e3', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'okol', 'text', NULL, NULL, 0, NULL, '2025-12-02 14:43:54', 0, 1, 1, 0, 0, NULL),
	('25fd9a76-1345-40ff-9362-627df21971d8', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'a', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:46:26', 0, 1, 1, 0, 0, NULL),
	('25fe122e-5253-4654-a545-db643e744679', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764992881/chat_images/chat_images/0a373e9a-460a-41e5-a2fa-1fa3209546af.jpg', '23IT.B066.jpg', 120646, NULL, '2025-12-06 03:48:01', 0, 1, 1, 0, 0, NULL),
	('28becdbc-456d-4658-890e-818194a14865', '171ed8c5-4bef-4350-955b-b3409d6c4e51', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, 'alo', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:55:28', 0, 1, 1, 0, 0, NULL),
	('2a0073da-74cc-4a99-93f2-5f383023331d', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'aaaaaaaaa', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:46:23', 0, 1, 1, 0, 0, NULL),
	('2a6d33a6-b7a3-4a75-813c-91418466a203', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'â', 'text', NULL, NULL, 0, NULL, '2025-12-03 13:40:35', 0, 1, 1, 0, 0, NULL),
	('2ab6d559-9daa-486e-869b-6207f13abcc2', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'aa', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:46:09', 0, 1, 1, 0, 0, NULL),
	('2b8be91f-18bb-4a6a-9416-6fd7f468e65a', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, 'ok', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:55:05', 0, 1, 1, 0, 0, NULL),
	('2e26d801-12f0-4fa5-b6bb-07a24a531fcd', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '', 'file', 'http://localhost:8080/uploads/documents/2504c56a-d434-47ea-a608-110b9a2f879d.docx', 'Trắc Nghiệm c1 tt.docx', 15667, NULL, '2025-12-04 13:17:19', 0, 1, 1, 0, 0, NULL),
	('2e643d15-fb09-4c36-a8a2-ce21deeb6fec', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764926052/chat_images/chat_images/fe139194-dbef-4e7e-acde-246b94c67b5c.png', 'Screenshot 2025-11-25 184936.png', 113266, NULL, '2025-12-05 09:14:12', 0, 1, 1, 0, 0, NULL),
	('34efc579-4539-4262-817d-9c1366a956bc', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, '', 'image', 'http://localhost:8080/uploads/images/d1f42f2a-e6a7-4ab9-8cd7-b85186e76d86.jpg', '22(1).jpg', 85006, NULL, '2025-12-04 13:16:11', 0, 1, 1, 0, 0, NULL),
	('35137f4d-5e02-4185-853d-4136e716d23f', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'â', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:48:03', 0, 1, 1, 0, 0, NULL),
	('355b17a8-b9ad-49f5-abfb-8d0a1349a10f', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'nô', 'text', NULL, NULL, 0, NULL, '2025-12-04 12:13:13', 0, 1, 1, 0, 0, NULL),
	('360453b1-1444-47d7-aa15-b804c7328d99', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'hê', 'text', NULL, NULL, 0, NULL, '2025-12-05 07:19:31', 0, 1, 1, 0, 0, NULL),
	('374bc0aa-4689-4acf-8d29-536c055432dc', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'haha', 'text', NULL, NULL, 0, NULL, '2025-12-06 04:36:48', 0, 1, 1, 0, 0, NULL),
	('386b83ae-bfa3-4325-929e-a99ed8a5e202', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764990732/chat_images/chat_images/3ef47587-1ffd-4f19-9b01-c871704f9642.png', 'Screenshot 2025-11-19 185339.png', 115992, NULL, '2025-12-06 03:12:11', 0, 1, 1, 0, 0, NULL),
	('38e5b39c-8dde-4699-9b8b-ba5f990e04ca', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'uuu', 'text', NULL, NULL, 0, NULL, '2025-12-05 09:29:29', 0, 1, 1, 0, 0, NULL),
	('3f28e945-c13c-43f5-8bda-11801c72f530', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'alooo', 'text', NULL, NULL, 0, NULL, '2025-12-04 12:09:19', 0, 1, 1, 0, 0, NULL),
	('4bfd3e6c-1fd5-4cdb-855c-6361b6a9dc38', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764992325/chat_images/chat_images/ae7bd74b-26e5-4fc3-ba5b-b2f29c538d30.png', 'Screenshot 2025-11-20 180223.png', 263014, NULL, '2025-12-06 03:38:46', 0, 1, 1, 0, 0, NULL),
	('4d37b3d4-4776-47b8-9ad8-fd287f986a52', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'huhu', 'text', NULL, NULL, 0, NULL, '2025-12-02 16:22:26', 0, 1, 1, 0, 0, NULL),
	('4de5b52b-823b-4094-ab34-f0e96d1790eb', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '', 'file', 'http://localhost:8080/uploads/documents/5253a866-9139-4edf-9120-25015bb089ab.pptx', 'Naive_Bayes_Presentation.pptx', 31511, NULL, '2025-12-04 13:17:11', 0, 1, 1, 0, 0, NULL),
	('568966ec-e170-4150-8fbb-8f28a2847e42', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764992300/chat_images/chat_images/8617ba87-86a1-4302-801c-a81df5448184.jpg', 'giam_gia_4.jpg', 10301, NULL, '2025-12-06 03:38:20', 0, 1, 1, 0, 0, NULL),
	('57650f66-a2cf-4a5d-97d6-52e2752c517a', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764995466/chat_images/chat_images/78e473dc-e08c-4523-857d-1d48639d4790.png', 'sơ đồ lớp.png', 210983, NULL, '2025-12-06 04:31:06', 0, 1, 1, 0, 0, NULL),
	('5895613b-66fc-4390-86ac-9da4dc04ef4a', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '👍', 'like', NULL, NULL, 0, NULL, '2025-12-03 01:39:29', 0, 1, 1, 0, 0, NULL),
	('58a0847d-111d-4364-bc41-a45c812cda65', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, 'huu', 'text', NULL, NULL, 0, NULL, '2025-12-05 07:19:39', 0, 1, 1, 0, 0, NULL),
	('58c8562b-dc38-44e9-8aa9-506b1c608443', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'haha', 'text', NULL, NULL, 0, NULL, '2025-12-06 04:30:53', 0, 1, 1, 0, 0, NULL),
	('5cbbebd9-685a-4df9-8d98-01cfddd82d2b', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'haha', 'text', NULL, NULL, 0, NULL, '2025-12-06 03:38:06', 0, 1, 1, 0, 0, NULL),
	('5e61bbe2-2cda-4893-9424-25ca08a0dcc5', '171ed8c5-4bef-4350-955b-b3409d6c4e51', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, '', 'file', 'http://localhost:8080/uploads/documents/2813c43f-c858-4e79-9bfd-83168a5c9593.docx', 'Naive_Bayes_Report.docx', 37196, NULL, '2025-12-06 02:38:21', 0, 1, 1, 0, 0, NULL),
	('5f27b82a-0721-4266-8eb3-6146e714d09f', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'aaa', 'text', NULL, NULL, 0, NULL, '2025-12-04 11:58:02', 0, 1, 1, 0, 0, NULL),
	('6018d6d0-0ee3-4d16-88eb-abe093a53a07', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '👍', 'like', NULL, NULL, 0, NULL, '2025-12-06 05:25:55', 0, 1, 1, 0, 0, NULL),
	('6053d577-b3ce-41fb-b3f7-669ff5042ec8', '171ed8c5-4bef-4350-955b-b3409d6c4e51', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, 'hehehe', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:55:21', 0, 1, 1, 0, 0, NULL),
	('60f878c9-b6b5-44f2-813d-792519d29ffe', '171ed8c5-4bef-4350-955b-b3409d6c4e51', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'â', 'text', NULL, NULL, 0, NULL, '2025-12-06 03:42:20', 0, 1, 1, 0, 0, NULL),
	('615e7d9f-1132-4291-905d-0ea675a4567f', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764990165/chat_images/chat_images/c0f1920e-0610-40f8-a219-12aa24b9843d.png', 'Screenshot 2025-11-19 081020.png', 307266, NULL, '2025-12-06 03:02:45', 0, 1, 1, 0, 0, NULL),
	('648a30fb-6545-49bf-bfc5-a90571749078', '171ed8c5-4bef-4350-955b-b3409d6c4e51', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764992571/chat_images/chat_images/1724732d-252b-4075-b7b0-7a229d3db10f.jpg', '1000017628.jpg', 58961, NULL, '2025-12-06 03:42:51', 0, 1, 1, 0, 0, NULL),
	('664ebae9-3771-41a0-90cb-13b0f9d49c5f', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764928529/chat_images/chat_images/119b101d-ef56-4e71-bd46-aa1c5eb1784e.png', 'Screenshot 2025-11-19 083217.png', 145740, NULL, '2025-12-05 09:55:30', 0, 1, 1, 0, 0, NULL),
	('666af61d-eb69-4770-8b70-af2037d764cc', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'haha', 'text', NULL, NULL, 0, NULL, '2025-12-06 03:47:44', 0, 1, 1, 0, 0, NULL),
	('6926f881-0de7-41bf-a0c8-6eeabd81e015', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'a', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:46:24', 0, 1, 1, 0, 0, NULL),
	('6d211470-5e7e-4b2e-8d32-0ebf4de00f97', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764992081/chat_images/chat_images/3bc42fcc-93ef-4b8d-836c-6aa8a78c2202.png', 'Screenshot 2025-11-19 185123.png', 131683, NULL, '2025-12-06 03:34:41', 0, 1, 1, 0, 0, NULL),
	('6d60c002-5df9-4599-b797-25844960fdf4', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764991313/chat_images/chat_images/30580642-607a-4b66-90a8-09790ddfa219.png', 'Screenshot 2025-11-19 185123.png', 131683, NULL, '2025-12-06 03:21:53', 0, 1, 1, 0, 0, NULL),
	('6e289a75-2786-43d7-97cd-1ea556c29f6e', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'hahahahahaha', 'text', NULL, NULL, 0, NULL, '2025-12-04 12:19:21', 0, 1, 1, 0, 0, NULL),
	('6e33e721-5f70-48c3-9e5f-4e325a312ac3', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764989549/chat_images/chat_images/396c50fb-835f-413b-bf85-a57fbdee7299.jpg', '23IT.B066.jpg', 120646, NULL, '2025-12-06 02:52:29', 0, 1, 1, 0, 0, NULL),
	('7274b135-80b5-4971-a958-ded954c55166', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764994252/chat_images/chat_images/5bd260c1-4d66-4f45-962c-581adcf14c6d.png', 'Screenshot 2025-11-19 074100.png', 69734, NULL, '2025-12-06 04:10:52', 0, 1, 1, 0, 0, NULL),
	('735d7cef-0114-4564-8737-aaf0051d1503', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'hâ', 'text', NULL, NULL, 0, NULL, '2025-12-02 14:39:46', 0, 1, 1, 0, 0, NULL),
	('74f3e4cb-47ce-41df-a1bb-d47174544e7e', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'huuu', 'text', NULL, NULL, 0, NULL, '2025-12-04 11:57:59', 0, 1, 1, 0, 0, NULL),
	('752b38e8-70c3-4de5-9b9c-407169a98a2f', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'aaa', 'text', NULL, NULL, 0, NULL, '2025-12-05 09:29:26', 0, 1, 1, 0, 0, NULL),
	('7662a377-9717-45b8-b5c9-b143444a415d', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'haha', 'text', NULL, NULL, 0, NULL, '2025-12-06 03:06:56', 0, 1, 1, 0, 0, NULL),
	('766587eb-afcc-43c8-b167-7b60f59de89b', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'aa', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:46:22', 0, 1, 1, 0, 0, NULL),
	('769a95f0-adba-4ec7-8d59-b0a4d70cf679', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, 'aa', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:55:02', 0, 1, 1, 0, 0, NULL),
	('769bc222-4fc3-4d5f-8f42-57d05e2ca259', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'hu', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:45:28', 0, 1, 1, 0, 0, NULL),
	('776773d3-7c8d-4f0d-b82c-5cd3970aa54a', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'hahah', 'text', NULL, NULL, 0, NULL, '2025-12-05 09:29:14', 0, 1, 1, 0, 0, NULL),
	('7c8c4b20-5308-4f52-b5c2-4c38d3cbe0a0', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '', 'file', 'http://localhost:8080/uploads/documents/284cb738-b399-4bc5-80fc-ce34cd91204d.docx', 'Lab2 (1).docx', 2556666, NULL, '2025-12-04 13:17:41', 0, 1, 1, 0, 0, NULL),
	('7d99845b-d724-4162-b7a5-011bbad93169', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'haha', 'text', NULL, NULL, 0, NULL, '2025-12-05 09:55:11', 0, 1, 1, 0, 0, NULL),
	('7e2c9ede-1ad8-420d-96ce-010120b41bd4', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'huhh', 'text', NULL, NULL, 0, NULL, '2025-12-02 14:44:38', 0, 1, 1, 0, 0, NULL),
	('7f1ded05-086f-48d2-a122-d4990344a2c8', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'huu', 'text', NULL, NULL, 0, NULL, '2025-12-05 09:53:28', 0, 1, 1, 0, 0, NULL),
	('825ef51e-dc6a-4863-b06b-d8b7d0dae6c5', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764926881/chat_images/chat_images/ed838407-5b68-4ccc-b350-0985e8179479.jpg', 'z7245061158490_d57a4bd9d24955eb53465549380c661a.jpg', 315130, NULL, '2025-12-05 09:28:01', 0, 1, 1, 0, 0, NULL),
	('842ae903-ac83-457e-baaa-37b4a8457e72', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'mak', 'text', NULL, NULL, 0, NULL, '2025-12-05 09:29:33', 0, 1, 1, 0, 0, NULL),
	('88b23add-b797-4ef7-94a3-c34e19699b46', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764927394/chat_images/chat_images/08102a8f-dc11-46d1-9b11-9bdf03b3d328.png', 'Screenshot 2025-12-03 230804.png', 1197363, NULL, '2025-12-05 09:36:34', 0, 1, 1, 0, 0, NULL),
	('8a0e26c6-64a0-4744-bd2c-a7c81520ed9d', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '', 'file', 'http://localhost:8080/uploads/documents/7274bc9e-6b35-478f-ac64-3f609b724016.docx', 'Naive_Bayes_Report.docx', 37196, NULL, '2025-12-04 13:15:26', 0, 1, 1, 0, 0, NULL),
	('8a8ce77e-f0e9-4571-9f92-b104814f31e4', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'mai nghỉ', 'text', NULL, NULL, 0, NULL, '2025-12-04 12:12:56', 0, 1, 1, 0, 0, NULL),
	('8d88c34b-d9ca-411c-9d81-15d4d66c00c1', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'haha', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:18:54', 0, 1, 1, 0, 0, NULL),
	('8e603aee-508c-4efa-b98d-e21929c05127', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'hah', 'text', NULL, NULL, 0, NULL, '2025-12-06 04:41:03', 0, 1, 1, 0, 0, NULL),
	('8fd95c97-c30d-4466-934e-31d9d7153615', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'haaa', 'text', NULL, NULL, 0, NULL, '2025-12-04 11:57:51', 0, 1, 1, 0, 0, NULL),
	('90221d5f-817a-4b34-890f-f9c0f197be05', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, '', 'STICKER', 'https://example.com/stickers/funny-faces/02.png', 'OMG', 0, NULL, '2025-12-06 07:39:39', 0, 0, 0, 0, 0, NULL),
	('94213825-2526-4511-9043-2b8b5864aaef', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, 'hello', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:55:12', 0, 1, 1, 0, 0, NULL),
	('997e9a97-bd1a-4d3f-91ff-a54d44a39a53', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, 'hhaa', 'text', NULL, NULL, 0, NULL, '2025-12-05 09:36:16', 0, 1, 1, 0, 0, NULL),
	('9ab02089-faf0-449d-a273-2608680d64b5', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764928419/chat_images/chat_images/e678398e-60fd-4650-9d63-8ca427eebbcf.png', 'Screenshot 2025-11-20 172005.png', 1015156, NULL, '2025-12-05 09:53:39', 0, 1, 1, 0, 0, NULL),
	('9d025d23-38ac-4a98-a33d-8c40b9baf6ee', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'haha', 'text', NULL, NULL, 0, NULL, '2025-12-06 02:52:10', 0, 1, 1, 0, 0, NULL),
	('9d77990b-57eb-464c-a00e-17c691081c82', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, 'ư', 'text', NULL, NULL, 0, NULL, '2025-12-05 10:04:33', 0, 1, 1, 0, 0, NULL),
	('9d9c4bb1-1dbe-4aee-ad59-2ffa7a5197f4', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, '😀', 'TEXT', NULL, NULL, 0, NULL, '2025-12-06 07:38:27', 0, 1, 1, 0, 0, NULL),
	('9dc7e065-52b2-4a62-b38e-e3381afc4bb1', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764989172/chat_images/chat_images/df31db88-debf-47a4-a062-c353e7d3a9a1.png', 'Screenshot 2025-11-19 074614.png', 275740, NULL, '2025-12-06 02:46:12', 0, 1, 1, 0, 0, NULL),
	('9e253608-1c5f-4d94-993d-edb391bc619c', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764992064/chat_images/chat_images/fe33c8a2-1c8c-462c-a6c0-b60afec87d00.png', 'Screenshot 2025-11-20 180207.png', 245996, NULL, '2025-12-06 03:34:24', 0, 1, 1, 0, 0, NULL),
	('9f48da1e-e88e-4359-a0f9-b44f2883b2e7', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764928028/chat_images/chat_images/bca3bbaa-7204-4996-bdc7-4c185fc6683b.png', 'Screenshot 2025-11-30 194034.png', 89872, NULL, '2025-12-05 09:47:08', 0, 1, 1, 0, 0, NULL),
	('a10db604-46ac-4ddc-b196-1009c8ae943a', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'huu', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:48:01', 0, 1, 1, 0, 0, NULL),
	('a255b48d-315a-4e79-8638-7e1b67d157ae', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764929082/chat_images/chat_images/078bd3ab-0667-4049-9761-19ee9e6eae5d.jpg', 'z7245061158490_d57a4bd9d24955eb53465549380c661a.jpg', 315130, NULL, '2025-12-05 10:04:42', 0, 1, 1, 0, 0, NULL),
	('a58d6767-2d03-47c3-9024-345bef83da79', '171ed8c5-4bef-4350-955b-b3409d6c4e51', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'alo', 'text', NULL, NULL, 0, NULL, '2025-12-03 02:35:11', 0, 1, 1, 0, 0, NULL),
	('a8786aae-1c83-4612-8dd3-dffe97ec7dee', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'hahaha', 'text', NULL, NULL, 0, NULL, '2025-12-04 12:12:35', 0, 1, 1, 0, 0, NULL),
	('aa4667f3-f6fe-408f-be25-0f5ecb51400f', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764926009/chat_images/chat_images/883c028d-2b90-4714-8fc6-cfe8661b8614.png', 'Screenshot 2025-11-20 190106.png', 1017453, NULL, '2025-12-05 09:13:30', 0, 1, 1, 0, 0, NULL),
	('aa56aeaa-ccde-4f33-a6fa-aa9cc4cbd682', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, 'â', 'text', NULL, NULL, 0, NULL, '2025-12-05 09:46:47', 0, 1, 1, 0, 0, NULL),
	('add6711c-98bf-4d57-bb2a-45528bbc6526', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '', 'image', 'http://localhost:8080/uploads/images/9bc3cb59-5cdf-49f3-8802-3de435f6e3fb.jpg', '23IT.B066.jpg', 120646, NULL, '2025-12-04 13:15:13', 0, 1, 1, 0, 0, NULL),
	('afce801a-212a-41ff-bb6c-01f61d534ca6', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764990433/chat_images/chat_images/e5c2f104-2388-4bf9-a8ce-5b77a97986fd.png', 'Screenshot 2025-11-20 182620.png', 108188, NULL, '2025-12-06 03:07:13', 0, 1, 1, 0, 0, NULL),
	('afe864df-f4ca-41af-a533-1e4a2f161ce5', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'haha', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:58:47', 0, 1, 1, 0, 0, NULL),
	('aff463b0-f660-4bae-b48e-4af715f74306', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764919027/chat_images/chat_images/2fbe0d83-f41c-4a07-8c09-e570f0d7388e.jpg', '23IT.B066.jpg', 120646, NULL, '2025-12-05 07:17:07', 0, 1, 1, 0, 0, NULL),
	('b02830cf-ae24-49d6-be53-e56adc81573f', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '', 'video', 'http://localhost:8080/uploads/videos/dea44170-4f42-4a22-8b5f-74fad9973fe2.mp4', 'Recording 2025-12-06 113809.mp4', 329255, NULL, '2025-12-06 04:38:20', 0, 1, 1, 0, 0, NULL),
	('b1e7c503-d2ce-4e87-8a23-09fc778b163c', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'haha', 'text', NULL, NULL, 0, NULL, '2025-12-06 04:10:00', 0, 1, 1, 0, 0, NULL),
	('b1f7c210-c462-42b9-a542-9f207c5112d3', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'huu', 'text', NULL, NULL, 0, NULL, '2025-12-05 09:55:15', 0, 1, 1, 0, 0, NULL),
	('b4561983-e8da-4902-8f5b-4399ff2b19fd', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'lô', 'text', NULL, NULL, 0, NULL, '2025-12-03 02:31:02', 0, 1, 1, 0, 0, NULL),
	('b47a77db-1a50-40b4-8b45-56c1965651a6', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764989139/chat_images/chat_images/0652f9d7-38c0-4f2c-9d3d-1a6a857340b2.jpg', 'z7245061158490_d57a4bd9d24955eb53465549380c661a.jpg', 315130, NULL, '2025-12-06 02:45:39', 0, 1, 1, 0, 0, NULL),
	('b4981161-880f-4562-b45f-4325b8f7917f', '171ed8c5-4bef-4350-955b-b3409d6c4e51', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, 'huu', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:55:24', 0, 1, 1, 0, 0, NULL),
	('b59cd444-cb1f-408d-ba8f-c43cdfc39244', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, 'aaaaaaaa', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:58:59', 0, 1, 1, 0, 0, NULL),
	('b6d37027-23f0-4845-b2da-e8b392eebd07', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'kk', 'text', NULL, NULL, 0, NULL, '2025-12-05 09:29:24', 0, 1, 1, 0, 0, NULL),
	('bbb812a6-2498-4ab3-af78-3139c1a20a77', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, 'huu', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:45:45', 0, 1, 1, 0, 0, NULL),
	('bf9af25f-2c9f-4236-9d65-2c3ba3c2abd6', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764919189/chat_images/chat_images/7ce659f4-dce8-40f0-90f1-e24550cebeb9.jpg', '23IT.B066.jpg', 120646, NULL, '2025-12-05 07:19:49', 0, 1, 1, 0, 0, NULL),
	('c0b52927-2ece-4b9b-8f01-0043786f376d', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'hha', 'text', NULL, NULL, 0, NULL, '2025-12-06 03:11:57', 0, 1, 1, 0, 0, NULL),
	('c3b090d3-b086-4790-8911-4911c9ccd4a5', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'alo', 'text', NULL, NULL, 0, NULL, '2025-12-04 11:57:40', 0, 1, 1, 0, 0, NULL),
	('c409421d-4ec2-4f8d-835c-7a831cb730d8', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'áasasas', 'text', NULL, NULL, 0, NULL, '2025-12-04 12:12:51', 0, 1, 1, 0, 0, NULL),
	('c6abd8ba-126e-46c0-a20d-72b6b8dfc7b1', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'huu', 'text', NULL, NULL, 0, NULL, '2025-12-05 09:29:20', 0, 1, 1, 0, 0, NULL),
	('c70ab084-3956-4e74-8b75-2623e29154e4', '171ed8c5-4bef-4350-955b-b3409d6c4e51', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764988662/chat_images/chat_images/2a4ae0ba-419a-4f67-a3ba-bee03a810203.jpg', '23IT.B066.jpg', 120646, NULL, '2025-12-06 02:37:42', 0, 1, 1, 0, 0, NULL),
	('c912e53b-9559-49f3-886b-e7f0c3705a14', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'ko', 'text', NULL, NULL, 0, NULL, '2025-12-04 12:13:00', 0, 1, 1, 0, 0, NULL),
	('ce172a9b-ea72-4b1f-80ef-cd0f3c14d71b', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764995835/chat_images/chat_images/c25305a6-b6d8-4c0e-8a60-518cc6f9b849.jpg', '0106_hinh-nen-4k-may-tinh4.jpg', 1744828, NULL, '2025-12-06 04:37:15', 0, 1, 1, 0, 0, NULL),
	('ce54ceec-be92-4001-b080-f54ab5f71728', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764995819/chat_images/chat_images/576f583a-ca47-4232-8536-c0251e2b7426.png', 'Screenshot 2025-11-20 184119.png', 15018, NULL, '2025-12-06 04:36:59', 0, 1, 1, 0, 0, NULL),
	('ce7c9776-5687-4f31-886d-eac94eaf4738', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'huu', 'text', NULL, NULL, 0, NULL, '2025-12-03 02:28:37', 0, 1, 1, 0, 0, NULL),
	('cea893ed-b640-49b3-a072-f1d3acb84603', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'mak', 'text', NULL, NULL, 0, NULL, '2025-12-05 09:29:34', 0, 1, 1, 0, 0, NULL),
	('d1ad598f-f2b0-4d90-a55a-c48e96b09c72', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'huu4', 'text', NULL, NULL, 0, NULL, '2025-12-04 12:09:33', 0, 1, 1, 0, 0, NULL),
	('d2176431-4c10-4459-98b2-c15b792da008', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '😴', 'TEXT', NULL, NULL, 0, NULL, '2025-12-06 07:38:54', 0, 1, 1, 0, 0, NULL),
	('d318281f-db50-4f9e-a5cf-06fcf9b0a4ff', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764993932/chat_images/chat_images/161145a4-a9c3-482b-807e-4c9f3c4d0a4b.jpg', 'z6470846619276_73edc28c26ac9cc428c9e349aceb5945.jpg', 14786, NULL, '2025-12-06 04:05:32', 0, 1, 1, 0, 0, NULL),
	('d320065b-894b-4025-9e7c-97906fbf0db6', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764991119/chat_images/chat_images/8610e420-bd3c-4fd2-a3d7-e1cc6839bc71.png', 'Screenshot 2025-11-20 173130.png', 16036, NULL, '2025-12-06 03:18:39', 0, 1, 1, 0, 0, NULL),
	('d719c364-8f50-4492-b9d9-851b9510b5a3', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '👍', 'like', NULL, NULL, 0, NULL, '2025-12-06 05:25:57', 0, 1, 1, 0, 0, NULL),
	('da042755-0926-4e34-94d6-f52216a47c8f', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'hello', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:46:16', 0, 1, 1, 0, 0, NULL),
	('dbae2b19-a1b4-430c-b290-e5d74099f6a1', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'hoangke', 'text', NULL, NULL, 0, NULL, '2025-12-02 16:22:24', 0, 1, 1, 0, 0, NULL),
	('de21aeb6-f8cd-4e7c-827a-7410d436ce5d', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, '😁', 'TEXT', NULL, NULL, 0, NULL, '2025-12-06 07:39:35', 0, 0, 0, 0, 0, NULL),
	('dfccb5e1-d4e1-4b8e-a266-6949ea402342', '171ed8c5-4bef-4350-955b-b3409d6c4e51', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764992555/chat_images/chat_images/5652cfec-c754-4c61-845b-29a6ab1859a7.png', 'Screenshot 2025-11-20 185324.png', 45536, NULL, '2025-12-06 03:42:35', 0, 1, 1, 0, 0, NULL),
	('e1244fad-ad80-425f-a627-0a1e631a3c9a', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'a', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:46:25', 0, 1, 1, 0, 0, NULL),
	('e13b7271-c300-464a-9fa0-c7985a4fc7ac', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'okok', 'text', NULL, NULL, 0, NULL, '2025-12-02 14:45:26', 0, 1, 1, 0, 0, NULL),
	('e24bdf8d-3d62-4958-b0ed-5456d3973484', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'noo', 'text', NULL, NULL, 0, NULL, '2025-12-04 12:12:40', 0, 1, 1, 0, 0, NULL),
	('e4146b81-f480-4bc5-b3a1-dcce552d66f1', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', NULL, 'xin chào', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:15:42', 0, 1, 1, 0, 0, NULL),
	('e5996fb3-5776-44d3-a01f-53bd1380024c', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'hhaha', 'text', NULL, NULL, 0, NULL, '2025-12-05 09:53:24', 0, 1, 1, 0, 0, NULL),
	('e5c72b56-8bf5-47a4-a626-862cf76b9f5b', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764995484/chat_images/chat_images/dd34282c-4421-4168-9e83-259a6439a5d5.jpg', 'images.jpg', 11709, NULL, '2025-12-06 04:31:24', 0, 0, 0, 0, 0, NULL),
	('e6f525db-7150-419e-924a-77fbd7b7fba9', '171ed8c5-4bef-4350-955b-b3409d6c4e51', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764992584/chat_images/chat_images/466d1e67-7f79-4192-9e9f-125dc0d6ed34.png', 'Home.png', 33789, NULL, '2025-12-06 03:43:04', 0, 1, 1, 0, 0, NULL),
	('e779fbc0-3404-413f-a943-48ae0fd6c54f', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764992114/chat_images/chat_images/5fdfb744-e0c3-44e5-b824-562773563be1.png', 'Screenshot 2025-11-19 185123.png', 131683, NULL, '2025-12-06 03:35:14', 0, 1, 1, 0, 0, NULL),
	('e8be4975-ad2f-4faf-ad93-bffcec7c112b', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'kkkk', 'text', NULL, NULL, 0, NULL, '2025-12-04 12:15:01', 0, 1, 1, 0, 0, NULL),
	('ea242f69-c064-4694-9edf-ea0c910cad7c', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, 'hahah', 'text', NULL, NULL, 0, NULL, '2025-12-06 03:02:17', 0, 1, 1, 0, 0, NULL),
	('eac3e6dd-10f0-4936-8319-80222e7fda03', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764918971/chat_images/chat_images/00803f22-1bf2-4748-8acb-9f06b97d0b3b.png', 'Screenshot 2025-12-05 134515.png', 110293, NULL, '2025-12-05 07:16:11', 0, 1, 1, 0, 0, NULL),
	('ec577267-1939-4d1c-b334-0dd86da53976', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764914068/chat_images/chat_images/73bdec58-2107-48fa-97a7-2f1abf1a3d1d.jpg', '23IT.B066.jpg', 120646, NULL, '2025-12-05 05:54:28', 0, 1, 1, 0, 0, NULL),
	('f5ac67e1-b3fb-4dde-9663-a7e8f7cfa0af', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', NULL, '', 'image', 'https://res.cloudinary.com/dnkjhbw9m/image/upload/v1764990148/chat_images/chat_images/482b9734-e097-473c-bbbb-bc6767f9fe94.png', 'Screenshot 2025-11-19 084526.png', 183238, NULL, '2025-12-06 03:02:28', 0, 1, 1, 0, 0, NULL),
	('f752456e-38e6-4bee-9965-de5018deca28', 'b8249cfb-1892-4691-ad72-d3f99ac21bd7', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'dw', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:08:05', 0, 1, 1, 0, 0, NULL),
	('f779224f-633c-4811-9c10-2f96ad19a17e', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'â', 'text', NULL, NULL, 0, NULL, '2025-12-03 02:28:42', 0, 1, 1, 0, 0, NULL),
	('fec198bd-9f8c-470a-93aa-345e73904a56', '7683c268-bed6-4429-b1ad-0cfc98ec495d', '8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', NULL, 'aaaaaaaaaaaaaa', 'text', NULL, NULL, 0, NULL, '2025-12-03 12:19:15', 0, 1, 1, 0, 0, NULL);

-- Dumping structure for table chatapp.message_reactions
CREATE TABLE IF NOT EXISTS `message_reactions` (
  `message_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `emoji` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `timestamp` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`message_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `message_reactions_ibfk_1` FOREIGN KEY (`message_id`) REFERENCES `messages` (`message_id`) ON DELETE CASCADE,
  CONSTRAINT `message_reactions_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table chatapp.message_reactions: ~0 rows (approximately)

-- Dumping structure for table chatapp.notifications
CREATE TABLE IF NOT EXISTS `notifications` (
  `notification_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content` text COLLATE utf8mb4_unicode_ci,
  `reference_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_read` tinyint(1) DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `data` json DEFAULT NULL,
  PRIMARY KEY (`notification_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `notifications_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table chatapp.notifications: ~0 rows (approximately)

-- Dumping structure for table chatapp.password_reset_tokens
CREATE TABLE IF NOT EXISTS `password_reset_tokens` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `token` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` timestamp NOT NULL,
  `is_used` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `token` (`token`),
  KEY `user_id` (`user_id`),
  KEY `idx_token` (`token`),
  KEY `idx_expires_at` (`expires_at`),
  CONSTRAINT `password_reset_tokens_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table chatapp.password_reset_tokens: ~0 rows (approximately)

-- Dumping structure for table chatapp.sessions
CREATE TABLE IF NOT EXISTS `sessions` (
  `session_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `token` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `login_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `last_activity_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `expiry_time` datetime NOT NULL,
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`session_id`),
  KEY `idx_user_active` (`user_id`,`is_active`),
  KEY `idx_token` (`token`(255)),
  CONSTRAINT `sessions_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table chatapp.sessions: ~0 rows (approximately)

-- Dumping structure for table chatapp.stickers
CREATE TABLE IF NOT EXISTS `stickers` (
  `sticker_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `pack_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tags` text COLLATE utf8mb4_unicode_ci,
  `order_index` int DEFAULT '0',
  `is_animated` tinyint(1) DEFAULT '0',
  `file_size` bigint DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`sticker_id`),
  KEY `idx_pack` (`pack_id`),
  KEY `idx_order` (`order_index`),
  KEY `idx_stickers_animated` (`is_animated`),
  FULLTEXT KEY `idx_sticker_search` (`name`,`tags`),
  CONSTRAINT `stickers_ibfk_1` FOREIGN KEY (`pack_id`) REFERENCES `sticker_packs` (`pack_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table chatapp.stickers: ~16 rows (approximately)
INSERT INTO `stickers` (`sticker_id`, `pack_id`, `image_url`, `name`, `tags`, `order_index`, `is_animated`, `file_size`, `created_at`) VALUES
	('s001', 'pack_001', 'https://example.com/stickers/cute-animals/01.png', 'Cat Happy', 'cat,happy,smile', 1, 0, 0, '2025-12-06 07:16:21'),
	('s002', 'pack_001', 'https://example.com/stickers/cute-animals/02.png', 'Dog Smile', 'dog,smile,joy', 2, 0, 0, '2025-12-06 07:16:21'),
	('s003', 'pack_001', 'https://example.com/stickers/cute-animals/03.png', 'Bear Hug', 'bear,hug,love', 3, 0, 0, '2025-12-06 07:16:21'),
	('s004', 'pack_001', 'https://example.com/stickers/cute-animals/04.png', 'Rabbit Love', 'rabbit,love,heart', 4, 0, 0, '2025-12-06 07:16:21'),
	('s005', 'pack_001', 'https://example.com/stickers/cute-animals/05.png', 'Panda Cry', 'panda,cry,sad', 5, 0, 0, '2025-12-06 07:16:21'),
	('s006', 'pack_001', 'https://example.com/stickers/cute-animals/06.png', 'Fox Laugh', 'fox,laugh,funny', 6, 0, 0, '2025-12-06 07:16:21'),
	('s007', 'pack_001', 'https://example.com/stickers/cute-animals/07.png', 'Koala Sleep', 'koala,sleep,tired', 7, 0, 0, '2025-12-06 07:16:21'),
	('s008', 'pack_001', 'https://example.com/stickers/cute-animals/08.png', 'Penguin Dance', 'penguin,dance,party', 8, 0, 0, '2025-12-06 07:16:21'),
	('s009', 'pack_002', 'https://example.com/stickers/funny-faces/01.png', 'LOL', 'laugh,lol,funny', 1, 0, 0, '2025-12-06 07:16:21'),
	('s010', 'pack_002', 'https://example.com/stickers/funny-faces/02.png', 'OMG', 'omg,shock,surprise', 2, 0, 0, '2025-12-06 07:16:21'),
	('s011', 'pack_002', 'https://example.com/stickers/funny-faces/03.png', 'Angry', 'angry,mad,upset', 3, 0, 0, '2025-12-06 07:16:21'),
	('s012', 'pack_002', 'https://example.com/stickers/funny-faces/04.png', 'Shocked', 'shock,wow,surprise', 4, 0, 0, '2025-12-06 07:16:21'),
	('s013', 'pack_002', 'https://example.com/stickers/funny-faces/05.png', 'Cool', 'cool,sunglasses,swag', 5, 0, 0, '2025-12-06 07:16:21'),
	('s014', 'pack_002', 'https://example.com/stickers/funny-faces/06.png', 'Thinking', 'think,hmm,wonder', 6, 0, 0, '2025-12-06 07:16:21'),
	('s015', 'pack_002', 'https://example.com/stickers/funny-faces/07.png', 'Crying', 'cry,sad,tears', 7, 0, 0, '2025-12-06 07:16:21'),
	('s016', 'pack_002', 'https://example.com/stickers/funny-faces/08.png', 'Party', 'party,celebrate,happy', 8, 0, 0, '2025-12-06 07:16:21');

-- Dumping structure for table chatapp.sticker_packs
CREATE TABLE IF NOT EXISTS `sticker_packs` (
  `pack_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `thumbnail_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `author` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_premium` tinyint(1) DEFAULT '0',
  `price` decimal(10,2) DEFAULT '0.00',
  `description` text COLLATE utf8mb4_unicode_ci,
  `download_count` int DEFAULT '0',
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`pack_id`),
  KEY `idx_premium` (`is_premium`),
  KEY `idx_active` (`is_active`),
  KEY `idx_created` (`created_at`),
  KEY `idx_sticker_packs_download` (`download_count` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table chatapp.sticker_packs: ~5 rows (approximately)
INSERT INTO `sticker_packs` (`pack_id`, `name`, `thumbnail_url`, `author`, `is_premium`, `price`, `description`, `download_count`, `is_active`, `created_at`, `updated_at`) VALUES
	('pack_001', 'Cute Animals', 'https://example.com/stickers/cute-animals/thumb.png', 'Sticker Team', 0, 0.00, 'Adorable animal stickers for everyday use', 0, 1, '2025-12-06 07:16:21', '2025-12-06 07:16:21'),
	('pack_002', 'Funny Faces', 'https://example.com/stickers/funny-faces/thumb.png', 'Emoji Art', 0, 0.00, 'Express yourself with funny emotions', 0, 1, '2025-12-06 07:16:21', '2025-12-06 07:16:21'),
	('pack_003', 'Love & Hearts', 'https://example.com/stickers/love-hearts/thumb.png', 'Love Studio', 0, 0.00, 'Spread love with romantic stickers', 0, 1, '2025-12-06 07:16:21', '2025-12-06 07:16:21'),
	('pack_004', 'Popular Memes', 'https://example.com/stickers/memes/thumb.png', 'Meme Central', 1, 2.99, 'Trending meme stickers pack', 0, 1, '2025-12-06 07:16:21', '2025-12-06 07:16:21'),
	('pack_005', 'Yummy Food', 'https://example.com/stickers/food/thumb.png', 'Foodie Art', 0, 0.00, 'Delicious food stickers', 0, 1, '2025-12-06 07:16:21', '2025-12-06 07:16:21');

-- Dumping structure for table chatapp.sticker_usage
CREATE TABLE IF NOT EXISTS `sticker_usage` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sticker_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `used_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `sticker_id` (`sticker_id`),
  KEY `idx_user_sticker` (`user_id`,`sticker_id`),
  KEY `idx_used_at` (`used_at`),
  CONSTRAINT `sticker_usage_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `sticker_usage_ibfk_2` FOREIGN KEY (`sticker_id`) REFERENCES `stickers` (`sticker_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table chatapp.sticker_usage: ~0 rows (approximately)

-- Dumping structure for table chatapp.users
CREATE TABLE IF NOT EXISTS `users` (
  `user_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `salt` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avatar_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bio` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'offline',
  `status_message` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_verified` tinyint(1) DEFAULT '0',
  `is_active` tinyint(1) DEFAULT '1',
  `is_online` tinyint(1) DEFAULT '0',
  `last_seen` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_username` (`username`),
  KEY `idx_email` (`email`),
  KEY `idx_is_online` (`is_online`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table chatapp.users: ~3 rows (approximately)
INSERT INTO `users` (`user_id`, `username`, `email`, `password_hash`, `salt`, `display_name`, `phone`, `avatar_url`, `bio`, `status`, `status_message`, `is_verified`, `is_active`, `is_online`, `last_seen`, `created_at`, `updated_at`) VALUES
	('303b96bb-ff0f-4b85-9fbe-e42f96c2bbd3', 'hoang', 'nguyenvanhoang09092005@gmail.com', 'msLftmwcWcgpIY72Zq17V4wQzb2d5NqfvdYSOq8pvAk=', 'KOMmiprhnA6DTCNZXHsKnQ==', 'hoang', NULL, NULL, NULL, 'offline', NULL, 1, 1, 0, '2025-12-05 21:36:09', '2025-11-27 04:40:33', '2025-12-05 21:36:09'),
	('6c5bdf72-ff94-4a96-9b7f-1bc815f1ef01', 'hoangpro', 'hoangnv3.23itb@vku.udn.vn', 'OhBtYs3apfEFMBQw3PvLImC+EZGUtE6cZEW6oDwDHuk=', '/4q9/HaboqHApLhqf3VLdg==', 'hoangpro', NULL, NULL, NULL, 'offline', NULL, 1, 1, 0, '2025-12-06 00:39:52', '2025-11-22 05:56:54', '2025-12-06 00:39:52'),
	('8bdb744d-4ea2-40e2-84eb-5b59e0e2552c', 'hoangke', 'nguyenvanhoang19092005@gmail.com', '5Q+dLRJ5q/CjWMwVctwEXlijfXqNbuXlPP8aqxrC1fk=', 'RQbsvcPuhE7tC/YSStbbpg==', 'hoangke', NULL, NULL, NULL, 'offline', NULL, 1, 1, 0, '2025-12-06 00:39:52', '2025-11-22 05:46:25', '2025-12-06 00:39:52');

-- Dumping structure for table chatapp.user_sticker_packs
CREATE TABLE IF NOT EXISTS `user_sticker_packs` (
  `user_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `pack_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `purchased_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `price_paid` decimal(10,2) DEFAULT '0.00',
  `transaction_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`user_id`,`pack_id`),
  KEY `pack_id` (`pack_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_purchased` (`purchased_at`),
  CONSTRAINT `user_sticker_packs_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `user_sticker_packs_ibfk_2` FOREIGN KEY (`pack_id`) REFERENCES `sticker_packs` (`pack_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table chatapp.user_sticker_packs: ~0 rows (approximately)

-- Dumping structure for table chatapp.verification_codes
CREATE TABLE IF NOT EXISTS `verification_codes` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `code` varchar(6) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` timestamp NOT NULL,
  `is_used` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `idx_email_code` (`email`,`code`),
  KEY `idx_expires_at` (`expires_at`),
  CONSTRAINT `verification_codes_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table chatapp.verification_codes: ~0 rows (approximately)

-- Dumping structure for trigger chatapp.trg_update_pack_downloads
SET @OLDTMP_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';
DELIMITER //
CREATE TRIGGER `trg_update_pack_downloads` AFTER INSERT ON `user_sticker_packs` FOR EACH ROW BEGIN
    UPDATE sticker_packs 
    SET download_count = download_count + 1 
    WHERE pack_id = NEW.pack_id;
END//
DELIMITER ;
SET SQL_MODE=@OLDTMP_SQL_MODE;

-- Dumping structure for trigger chatapp.trg_update_pack_timestamp
SET @OLDTMP_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';
DELIMITER //
CREATE TRIGGER `trg_update_pack_timestamp` BEFORE UPDATE ON `sticker_packs` FOR EACH ROW BEGIN
    SET NEW.updated_at = CURRENT_TIMESTAMP;
END//
DELIMITER ;
SET SQL_MODE=@OLDTMP_SQL_MODE;

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
