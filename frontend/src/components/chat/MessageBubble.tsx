import { ChatMessageResponse } from "@/types/chat";

interface MessageBubbleProps {
    message: ChatMessageResponse;
}

export default function MessageBubble({ message }: MessageBubbleProps) {
    const { host, content, createdAt } = message;
    const isHost = host;

    // 시간 포맷 (예: "오후 4:02")
    const formattedTime = new Date(createdAt).toLocaleTimeString("ko-KR", {
        hour: "numeric",
        minute: "2-digit",
        hour12: true,
    });

    // 말풍선 색상/스타일
    const bubbleClass = isHost
        ? "bg-white border border-gray-200 text-gray-800 rounded-tl-none"
        : "bg-gray-800 text-white rounded-tr-none";

    return (
        <div className={`flex ${isHost ? "justify-start" : "justify-end"} mb-3`}>
            <div className="flex flex-col max-w-[70%]">
                {isHost && (
                    <span className="text-xs text-gray-500 mb-1 ml-1">주최자</span>
                )}
                <div className={`px-4 py-2 rounded-2xl text-sm ${bubbleClass}`}>
                    {content}
                </div>
                <span
                    className={`text-xs text-gray-400 mt-1 ${
                        isHost ? "text-left ml-1" : "text-right mr-1"
                    }`}
                >
          {formattedTime}
        </span>

            </div>
        </div>
    );
}