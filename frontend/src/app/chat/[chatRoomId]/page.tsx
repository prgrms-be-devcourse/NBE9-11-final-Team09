import ChatRoom from "@/components/chat/ChatRoom";

interface ChatPageProps {
    params: {
        chatRoomId: string;
    };
    searchParams: {
        title?: string;
    };
}

export default function ChatPage({ params, searchParams }: ChatPageProps) {
    const chatRoomId = Number(params.chatRoomId);
    if (Number.isNaN(chatRoomId)) {
        return <div>잘못된 채팅방입니다.</div>;
    }

    const fundingTitle = searchParams.title ?? "채팅방";

    return (
        <ChatRoom
            chatRoomId={chatRoomId}
            fundingTitle={fundingTitle}
        />
    );
}