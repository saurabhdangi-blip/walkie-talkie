import { createSenderPeer } from "./webrtc";

export function usePushToTalk(socket, targetUserId) {
  let sender = null;

  const start = async () => {
    sender = await createSenderPeer(socket, targetUserId);
  };

  const stop = () => {
    sender?.stop();
    sender = null;
  };

  return { start, stop };
}
