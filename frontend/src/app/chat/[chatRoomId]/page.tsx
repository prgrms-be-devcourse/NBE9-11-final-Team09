import ChatRoom from "@/components/chat/ChatRoom";

interface ChatPageProps {
    params: Promise<{
        chatRoomId: string;
    }>;
    searchParams: Promise<{
        title?: string;
    }>;
}

export default async function ChatPage({ params, searchParams }: ChatPageProps) {
    const { chatRoomId } = await params;
    const { title } = await searchParams;

    const chatRoomIdNum = Number(chatRoomId);

    if (Number.isNaN(chatRoomIdNum)) {
        return <div>잘못된 채팅방입니다.</div>;
    }

    const fundingTitle = title ?? "채팅방";

    return (
        <ChatRoom
            chatRoomId={chatRoomIdNum}
            fundingTitle={fundingTitle}
        />
    );
}