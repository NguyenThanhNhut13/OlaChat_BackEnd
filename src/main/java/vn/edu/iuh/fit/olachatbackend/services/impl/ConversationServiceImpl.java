/*
 * @ (#) ConversationServiceImpl.java       1.0     14/02/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package vn.edu.iuh.fit.olachatbackend.services.impl;
/*
 * @description:
 * @author: Nguyen Thanh Nhut
 * @date: 14/02/2025
 * @version:    1.0
 */

import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.olachatbackend.dtos.ConversationDTO;
import vn.edu.iuh.fit.olachatbackend.dtos.responses.ConversationResponse;
import vn.edu.iuh.fit.olachatbackend.dtos.responses.ParticipantResponse;
import vn.edu.iuh.fit.olachatbackend.dtos.responses.UserResponse;
import vn.edu.iuh.fit.olachatbackend.entities.*;
import vn.edu.iuh.fit.olachatbackend.enums.MessageType;
import vn.edu.iuh.fit.olachatbackend.enums.ParticipantRole;
import vn.edu.iuh.fit.olachatbackend.exceptions.BadRequestException;
import vn.edu.iuh.fit.olachatbackend.exceptions.NotFoundException;
import vn.edu.iuh.fit.olachatbackend.mappers.ConversationMapperImpl;
import vn.edu.iuh.fit.olachatbackend.mappers.ParticipantMapper;
import vn.edu.iuh.fit.olachatbackend.mappers.UserMapper;
import vn.edu.iuh.fit.olachatbackend.repositories.*;
import vn.edu.iuh.fit.olachatbackend.services.ConversationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {
    private final ConversationRepository conversationRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ConversationMapperImpl conversationMapperImpl;
    private final DeletedConversationRepository deletedConversationRepository;

    public ConversationDTO createConversation(ConversationDTO conversationDTO) {
        Conversation conversation = Conversation.builder()
                .name(conversationDTO.getName())
                .avatar(conversationDTO.getAvatar())
                .type(conversationDTO.getType())
                .lastMessage(conversationDTO.getLastMessage())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Conversation savedConversation = conversationRepository.save(conversation);

        List<Participant> participants = conversationDTO.getUserIds().stream()
                .map(userId -> Participant.builder()
                        .conversationId(savedConversation.getId())
                        .userId(userId)
                        .role(ParticipantRole.MEMBER)
                        .joinedAt(LocalDateTime.now())
                        .build())
                .toList();
        participantRepository.saveAll(participants);

        return conversationMapperImpl.toDTO(savedConversation);
    }

    @Override
    public List<ConversationResponse> getAllConversationsByUser() {
        User user = getCurrentUser();

        // Get all participants by user
        List<Participant> participants = participantRepository.findByUserId(user.getId());

        // Get list conversationId from participants
        List<ObjectId> conversationIds = participants.stream()
                .map(Participant::getConversationId)
                .distinct()
                .toList();

        // Get all conversations
        List<Conversation> conversations = conversationRepository.findByIdIn(conversationIds);

        // Get list of conversations the user has deleted
        List<DeletedConversation> deletedConversations = deletedConversationRepository.findByUserId(user.getId());
        Set<String> deletedConversationIds = deletedConversations.stream()
                .map(DeletedConversation::getConversationId)
                .collect(Collectors.toSet());

        // Create conversation response (exclude deleted)
        return conversations.stream()
                .filter(convo -> !deletedConversationIds.contains(convo.getId().toHexString()))
                .sorted((c1, c2) -> {
                    // If either one has no lastMessage, bring the one with lastMessage first.
                    if (c1.getLastMessage() == null && c2.getLastMessage() == null) return 0;
                    if (c1.getLastMessage() == null) return 1;
                    if (c2.getLastMessage() == null) return -1;

                    // Compare by createAt of lastMessage (newest to top)
                    return c2.getLastMessage().getCreatedAt().compareTo(c1.getLastMessage().getCreatedAt());
                })
                .map(conversation -> ConversationResponse.builder()
                        .id(conversation.getId() != null ? conversation.getId().toHexString() : null)
                        .name(conversation.getName())
                        .avatar(conversation.getAvatar())
                        .type(conversation.getType())
                        .lastMessage(conversation.getLastMessage())
                        .createdAt(conversation.getCreatedAt())
                        .updatedAt(conversation.getUpdatedAt())
                        .build())
                .toList();

    }


    @Override
    public void sendSystemMessageAndUpdateLast(String conversationId, String content) {
        // Create system message
        Message systemMessage = Message.builder()
                .conversationId(new ObjectId(conversationId))
                .senderId(null)
                .content(content)
                .createdAt(LocalDateTime.now())
                .type(MessageType.SYSTEM)
                .build();

        // Save message
        messageRepository.save(systemMessage);

        // Update lastMessage for conversation
        Conversation conversation = conversationRepository.findById(new ObjectId(conversationId))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy conversation!"));

        // Set lastMessage
        conversation.setLastMessage(LastMessage.builder()
                        .messageId(systemMessage.getId())
                        .content(content)
                        .createdAt(LocalDateTime.now())
                        .senderId(null)
                .build());

        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    @Override
    public void updateLastMessage(ObjectId conversationId, Message newLastMessage) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy conversation!"));

        conversation.setLastMessage(LastMessage.builder()
                .messageId(newLastMessage.getId())
                .content(getLastMessagePreview(newLastMessage))
                .createdAt(newLastMessage.getCreatedAt())
                .senderId(newLastMessage.getSenderId())
                .build());

        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    @Override
    public void softDeleteConversation(String userId, String conversationId) {
        if (!deletedConversationRepository.existsByUserIdAndConversationId(userId, conversationId)) {
            DeletedConversation deleted = DeletedConversation.builder()
                    .userId(userId)
                    .conversationId(conversationId)
                    .deletedAt(LocalDateTime.now())
                    .build();
            deletedConversationRepository.save(deleted);
        }
    }


    private String getLastMessagePreview(Message message) {
        if (message.isRecalled()) {
            return "[Tin nhắn đã thu hồi]";
        }

        return switch (message.getType()) {
            case TEXT, SYSTEM -> message.getContent();
            case MEDIA -> "[Đã gửi ảnh]";
            case FILE -> "[Đã gửi tệp tin]";
            case STICKER -> "[Sticker]";
            case EMOJI -> message.getContent(); // emoji unicode
            case VOICE -> "[Tin nhắn thoại]";
            default -> "[Tin nhắn]";
        };
    }

    private User getCurrentUser() {
        // Check user
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        return userRepository.findByUsername(name)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng này"));
    }

}
