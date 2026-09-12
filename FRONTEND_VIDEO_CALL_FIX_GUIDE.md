# Frontend Developer Guide: Fixing Video/Mic Auto-Off Bug & Removing "Google Meet" Branding

This guide is for the frontend developer working on **BingeTogether-Frontend**. It explains why the camera and microphone were turning off automatically, how to fix it, removes all "Google Meet" branding, and provides the complete, production-ready code.

---

## 1. Why Did the Camera & Mic Turn Off on Their Own? (The Bug)

### The Root Cause: React `useEffect` Dependency Trap

In `RoomPage.jsx`, the STOMP WebSocket connection was set up inside this `useEffect`:

```javascript
// ❌ THE BUGGY IMPLEMENTATION
useEffect(() => {
  const client = new Client({ ... });
  client.activate();

  return () => {
    leaveMeeting(); // 👈 This stops all camera/mic tracks!
    if (stompClientRef.current) stompClientRef.current.deactivate();
  };
}, [roomCode, currentUserId, currentUsername, isInCall, isCameraOn, isMuted]); 
// 👆 BUG: isInCall, isCameraOn, isMuted are in the dependency array!
```

### What Happened When You Clicked "Turn Camera On":
1. You clicked **Turn Camera On** $\rightarrow$ `setIsCameraOn(true)` ran.
2. React re-rendered `RoomPage`.
3. Because `isCameraOn` changed, React **ran the cleanup function** of the previous `useEffect` before running the next one.
4. The cleanup function called **`leaveMeeting()`**!
5. `leaveMeeting()` executed:
   ```javascript
   localStreamRef.current.getTracks().forEach((track) => track.stop()); // Kills camera!
   setIsCameraOn(false); // Resets state back to false!
   setIsMuted(false);
   ```
6. **Result**: Your camera turned on for a split second, and then React immediately turned it off and destroyed your WebRTC connection.

---

## 2. The Solution: Use React `useRef` for Media States

1. **Keep the STOMP `useEffect` focused only on the room lifecycle**:
   Its dependency array should **ONLY** contain `[roomCode, currentUserId, currentUsername]`.
2. **Use Refs for live state tracking**:
   Maintain `isInCallRef`, `isCameraOnRef`, and `isMutedRef`. This allows WebSocket callbacks to read the current camera/mic status without triggering a component re-subscription or tearing down the media stream.

```javascript
// ✅ THE FIX
const isInCallRef = useRef(false);
const isCameraOnRef = useRef(false);
const isMutedRef = useRef(false);

useEffect(() => { isInCallRef.current = isInCall; }, [isInCall]);
useEffect(() => { isCameraOnRef.current = isCameraOn; }, [isCameraOn]);
useEffect(() => { isMutedRef.current = isMuted; }, [isMuted]);

// Now STOMP effect ONLY runs on mount and unmount:
useEffect(() => {
  // Setup STOMP WebSocket...
  return () => {
    leaveMeeting();
    stompClientRef.current?.deactivate();
  };
}, [roomCode, currentUserId, currentUsername]); // 👈 STABLE! Never triggers on camera/mic toggle!
```

---

## 3. Brand Cleanup: Remove "Google Meet" Mentions

Remove all references to "Google Meet" and replace them with native watch-party branding:
- ❌ `<span className="meet-mode-tag">Google Meet Mode</span>` $\rightarrow$ ✅ `<span className="party-cam-tag">Live</span>`
- ❌ `<h3>Live Call & Video Meeting</h3>` $\rightarrow$ ✅ `<h3>Party Cam & Voice</h3>`
- ❌ `.google-meet-avatar` $\rightarrow$ ✅ `.party-avatar-circle`
- ❌ `.google-meet-stage` $\rightarrow$ ✅ `.party-cam-stage`

---

## 4. Complete Ready-to-Use Code

### File 1: `src/pages/RoomPage.jsx`

Replace the entire contents of **`src/pages/RoomPage.jsx`** with the following:

```jsx
import { useEffect, useState, useRef, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import "./RoomPage.css";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { API_BASE_URL, WS_BASE_URL } from "../config";
import CustomVideoPlayer from "../components/CustomVideoPlayer";

// Free Google STUN Servers
const RTC_CONFIG = {
  iceServers: [
    { urls: "stun:stun.l.google.com:19302" },
    { urls: "stun:stun1.l.google.com:19302" },
  ],
};

/**
 * Participant Video Tile Subcomponent
 * Displays live video when camera is ON, or an Avatar placeholder when camera is OFF.
 * Remote audio plays seamlessly without echoes.
 */
function ParticipantVideoTile({
  stream,
  name,
  isLocal,
  isCameraOn,
  isMuted,
  avatarInitial,
}) {
  const videoRef = useRef(null);
  const audioRef = useRef(null);

  useEffect(() => {
    if (videoRef.current && stream && isCameraOn) {
      if (videoRef.current.srcObject !== stream) {
        videoRef.current.srcObject = stream;
      }
    }
  }, [stream, isCameraOn]);

  useEffect(() => {
    // When camera is off for a remote user, their mic audio still plays via an audio element
    if (!isLocal && !isCameraOn && audioRef.current && stream) {
      if (audioRef.current.srcObject !== stream) {
        audioRef.current.srcObject = stream;
      }
    }
  }, [stream, isCameraOn, isLocal]);

  return (
    <div
      className={`participant-tile ${isLocal ? "local-participant" : ""} ${
        !isCameraOn ? "camera-off" : ""
      }`}
    >
      {/* 1. Live Video when Camera is ON */}
      {isCameraOn && stream ? (
        <video
          ref={videoRef}
          autoPlay
          playsInline
          muted={isLocal} // MUST be muted for local to prevent acoustic feedback loop!
          className={`participant-video ${isLocal ? "mirrored" : ""}`}
        />
      ) : (
        /* 2. Avatar Placeholder when Camera is OFF */
        <div className="avatar-placeholder-container">
          <div className="party-avatar-circle">
            {avatarInitial || (name ? name.charAt(0).toUpperCase() : "U")}
          </div>
          <span className="camera-off-indicator">Camera Off</span>
        </div>
      )}

      {/* Hidden audio element for remote participants when their camera is off but mic is on */}
      {!isLocal && !isCameraOn && (
        <audio ref={audioRef} autoPlay playsInline />
      )}

      {/* Bottom Status Bar */}
      <div className="tile-bottom-bar">
        <span className="participant-name">
          {name} {isLocal && "(You)"}
        </span>
        <span
          className={`mic-badge ${isMuted ? "muted" : "unmuted"}`}
          title={isMuted ? "Microphone Muted" : "Microphone Active"}
        >
          {isMuted ? "🔇" : "🎙️"}
        </span>
      </div>
    </div>
  );
}

function RoomPage() {
  const { roomCode } = useParams();
  const navigate = useNavigate();
  const messagesEndRef = useRef(null);
  const botMessagesEndRef = useRef(null);

  const playerRef = useRef(null);
  const isSeekingRef = useRef(false);
  const ignoreNextSyncRef = useRef(false);

  const currentUserId = Number(localStorage.getItem("userId")) || 999;
  const currentUsername = localStorage.getItem("username")?.trim() || "User";

  const [room, setRoom] = useState(null);
  const [members, setMembers] = useState([]);
  const [messages, setMessages] = useState([]);
  const [message, setMessage] = useState("");
  const [copied, setCopied] = useState(false);
  const stompClientRef = useRef(null);

  const [pendingSync, setPendingSync] = useState(null);
  const [demoVideo, setDemoVideo] = useState(null);

  // 🤖 BingeBot State & Controls
  const [activeTab, setActiveTab] = useState("chat");
  const [botMessages, setBotMessages] = useState([
    {
      id: 1,
      sender: "BingeBot",
      text: "Hey! I'm your private watch-party AI assistant. Ask me anything about this movie or scene!",
      isBot: true,
      timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
    },
  ]);
  const [botInput, setBotInput] = useState("");
  const [isBotLoading, setIsBotLoading] = useState(false);

  // 📹 Independent Media States
  const [isInCall, setIsInCall] = useState(false);
  const [isCameraOn, setIsCameraOn] = useState(false);
  const [isMuted, setIsMuted] = useState(false);

  // REFS TO PREVENT STOMP EFFECT TEARDOWN ON STATE CHANGES
  const isInCallRef = useRef(false);
  const isCameraOnRef = useRef(false);
  const isMutedRef = useRef(false);

  useEffect(() => { isInCallRef.current = isInCall; }, [isInCall]);
  useEffect(() => { isCameraOnRef.current = isCameraOn; }, [isCameraOn]);
  useEffect(() => { isMutedRef.current = isMuted; }, [isMuted]);

  const [localStream, setLocalStream] = useState(null);
  const [remoteStreams, setRemoteStreams] = useState({}); // { [peerId]: MediaStream }
  const [peerMediaStates, setPeerMediaStates] = useState({});

  const peerConnectionsRef = useRef({}); // { [peerId]: RTCPeerConnection }
  const pendingCandidatesRef = useRef({}); // { [peerId]: RTCIceCandidateInit[] }
  const localStreamRef = useRef(null);

  // Local storage mapping for persistent member names
  const getStoredRoomNames = () => {
    try {
      const saved = localStorage.getItem(`room_names_${roomCode}`);
      return saved ? JSON.parse(saved) : {};
    } catch {
      return {};
    }
  };

  const saveStoredRoomName = (userId, username) => {
    if (!userId || !username || username === "User" || username.startsWith("User #")) return;
    try {
      const currentMap = getStoredRoomNames();
      currentMap[Number(userId)] = username;
      localStorage.setItem(`room_names_${roomCode}`, JSON.stringify(currentMap));
    } catch (e) {
      console.error(e);
    }
  };

  useEffect(() => {
    if (currentUserId && currentUsername) {
      saveStoredRoomName(currentUserId, currentUsername);
    }
  }, [roomCode, currentUserId, currentUsername]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  const scrollBotToBottom = () => {
    botMessagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  // Broadcasts media state (camera on/off, mic on/off) over STOMP
  const broadcastMediaState = useCallback(
    (cameraState, mutedState) => {
      if (stompClientRef.current?.connected) {
        stompClientRef.current.publish({
          destination: `/app/room/${roomCode}/sync`,
          body: JSON.stringify({
            sender: currentUsername,
            userId: currentUserId,
            action: "MEDIA_STATE_UPDATE",
            isCameraOn: cameraState,
            isMuted: mutedState,
          }),
        });
      }
    },
    [roomCode, currentUsername, currentUserId]
  );

  // -------------------------------------------------------------
  // 🎙️ WebRTC Engine
  // -------------------------------------------------------------

  const getOrCreatePeerConnection = useCallback(
    (targetUserId) => {
      if (peerConnectionsRef.current[targetUserId]) {
        return peerConnectionsRef.current[targetUserId];
      }

      const pc = new RTCPeerConnection(RTC_CONFIG);

      // Add local tracks if available
      if (localStreamRef.current) {
        localStreamRef.current.getTracks().forEach((track) => {
          pc.addTrack(track, localStreamRef.current);
        });
      }

      // If no local tracks exist, ensure recvonly transceivers are ready to receive
      const senders = pc.getSenders();
      const hasAudio = senders.some((s) => s.track && s.track.kind === "audio");
      const hasVideo = senders.some((s) => s.track && s.track.kind === "video");

      if (!hasAudio) {
        pc.addTransceiver("audio", { direction: "recvonly" });
      }
      if (!hasVideo) {
        pc.addTransceiver("video", { direction: "recvonly" });
      }

      // Receive incoming tracks (audio & video)
      pc.ontrack = (event) => {
        const [stream] = event.streams;
        if (stream) {
          setRemoteStreams((prev) => ({ ...prev, [targetUserId]: stream }));
        } else if (event.track) {
          setRemoteStreams((prev) => {
            const current = prev[targetUserId] || new MediaStream();
            current.addTrack(event.track);
            return { ...prev, [targetUserId]: current };
          });
        }
      };

      // Relay ICE candidates
      pc.onicecandidate = (event) => {
        if (event.candidate && stompClientRef.current?.connected) {
          stompClientRef.current.publish({
            destination: `/app/room/${roomCode}/webrtc/candidate`,
            body: JSON.stringify({
              senderId: currentUserId,
              targetId: targetUserId,
              candidate: event.candidate,
            }),
          });
        }
      };

      peerConnectionsRef.current[targetUserId] = pc;
      return pc;
    },
    [roomCode, currentUserId]
  );

  // Drain any queued ICE candidates after remote description is set
  const drainIceCandidates = async (peerId, pc) => {
    const queue = pendingCandidatesRef.current[peerId] || [];
    for (const cand of queue) {
      try {
        await pc.addIceCandidate(new RTCIceCandidate(cand));
      } catch (err) {
        console.error("Error adding queued ICE candidate:", err);
      }
    }
    pendingCandidatesRef.current[peerId] = [];
  };

  // Call a peer: creates Offer and sends via STOMP
  const callPeer = async (targetUserId) => {
    try {
      const pc = getOrCreatePeerConnection(targetUserId);
      const offer = await pc.createOffer({
        offerToReceiveAudio: true,
        offerToReceiveVideo: true,
      });
      await pc.setLocalDescription(offer);

      if (stompClientRef.current?.connected) {
        stompClientRef.current.publish({
          destination: `/app/room/${roomCode}/webrtc/offer`,
          body: JSON.stringify({
            senderId: currentUserId,
            targetId: targetUserId,
            offer: offer,
          }),
        });
      }
    } catch (err) {
      console.error(`Failed to call peer ${targetUserId}:`, err);
    }
  };

  /**
   * Join the Meeting:
   * Initializes microphone without forcing camera ON!
   */
  const joinMeeting = async () => {
    try {
      let stream = null;
      try {
        stream = await navigator.mediaDevices.getUserMedia({
          audio: true,
          video: false,
        });
      } catch (audioErr) {
        console.warn("Could not capture microphone, joining in watch-only mode:", audioErr);
        stream = new MediaStream();
      }

      localStreamRef.current = stream;
      setLocalStream(stream);
      setIsInCall(true);
      isInCallRef.current = true;
      setIsCameraOn(false);
      isCameraOnRef.current = false;
      setIsMuted(false);
      isMutedRef.current = false;

      broadcastMediaState(false, false);

      if (stompClientRef.current?.connected) {
        stompClientRef.current.publish({
          destination: `/app/room/${roomCode}/sync`,
          body: JSON.stringify({
            sender: currentUsername,
            userId: currentUserId,
            action: "MEETING_JOINED",
          }),
        });
      }
    } catch (err) {
      console.error("Failed to join meeting:", err);
    }
  };

  /**
   * Leave Meeting:
   * Stops tracks and cleans up peer connections.
   */
  const leaveMeeting = () => {
    if (localStreamRef.current) {
      localStreamRef.current.getTracks().forEach((track) => track.stop());
      localStreamRef.current = null;
    }
    setLocalStream(null);
    setIsInCall(false);
    isInCallRef.current = false;
    setIsCameraOn(false);
    isCameraOnRef.current = false;
    setIsMuted(false);
    isMutedRef.current = false;

    Object.values(peerConnectionsRef.current).forEach((pc) => pc.close());
    peerConnectionsRef.current = {};
    pendingCandidatesRef.current = {};
    setRemoteStreams({});

    if (stompClientRef.current?.connected) {
      stompClientRef.current.publish({
        destination: `/app/room/${roomCode}/sync`,
        body: JSON.stringify({
          sender: currentUsername,
          userId: currentUserId,
          action: "MEETING_LEFT",
        }),
      });
    }
  };

  /**
   * Independent Camera Toggle:
   * Turns camera ON or OFF independently without affecting microphone or resetting STOMP.
   */
  const toggleCamera = async () => {
    if (!isInCallRef.current) {
      setIsInCall(true);
      isInCallRef.current = true;
    }

    if (isCameraOnRef.current) {
      // Turn Camera OFF
      if (localStreamRef.current) {
        const videoTracks = localStreamRef.current.getVideoTracks();
        videoTracks.forEach((t) => {
          t.stop();
          localStreamRef.current.removeTrack(t);
        });

        // Replace track in peer connections with null
        Object.values(peerConnectionsRef.current).forEach((pc) => {
          const sender = pc.getSenders().find((s) => s.track && s.track.kind === "video");
          if (sender) {
            sender.replaceTrack(null).catch(() => {});
          }
        });
        setLocalStream(new MediaStream(localStreamRef.current.getTracks()));
      }

      setIsCameraOn(false);
      isCameraOnRef.current = false;
      broadcastMediaState(false, isMutedRef.current);
    } else {
      // Turn Camera ON
      try {
        const videoStream = await navigator.mediaDevices.getUserMedia({
          video: { width: { ideal: 640 }, height: { ideal: 360 } },
        });
        const newVideoTrack = videoStream.getVideoTracks()[0];

        if (!localStreamRef.current) {
          localStreamRef.current = new MediaStream();
        }
        localStreamRef.current.addTrack(newVideoTrack);
        setLocalStream(new MediaStream(localStreamRef.current.getTracks()));

        // Add or replace video track in all active peer connections
        for (const [peerId, pc] of Object.entries(peerConnectionsRef.current)) {
          const videoSender = pc.getSenders().find((s) => s.track?.kind === "video" || (!s.track && s.kind === "video"));
          if (videoSender) {
            await videoSender.replaceTrack(newVideoTrack);
          } else {
            pc.addTrack(newVideoTrack, localStreamRef.current);
          }
          await callPeer(Number(peerId));
        }

        setIsCameraOn(true);
        isCameraOnRef.current = true;
        broadcastMediaState(true, isMutedRef.current);
      } catch (err) {
        console.error("Failed to access camera:", err);
        alert("Camera permission denied or camera unavailable.");
      }
    }
  };

  /**
   * Independent Microphone Toggle:
   * Mutes/unmutes local microphone without affecting camera.
   */
  const toggleMute = () => {
    if (!isInCallRef.current) {
      joinMeeting();
      return;
    }

    if (localStreamRef.current) {
      const audioTracks = localStreamRef.current.getAudioTracks();
      if (audioTracks.length > 0) {
        const nextMuted = !isMutedRef.current;
        audioTracks.forEach((t) => (t.enabled = !nextMuted));
        setIsMuted(nextMuted);
        isMutedRef.current = nextMuted;
        broadcastMediaState(isCameraOnRef.current, nextMuted);
      } else {
        // If joined with no mic track, capture mic track now
        navigator.mediaDevices.getUserMedia({ audio: true }).then((micStream) => {
          const micTrack = micStream.getAudioTracks()[0];
          localStreamRef.current.addTrack(micTrack);
          setLocalStream(new MediaStream(localStreamRef.current.getTracks()));
          Object.values(peerConnectionsRef.current).forEach((pc) => {
            pc.addTrack(micTrack, localStreamRef.current);
          });
          setIsMuted(false);
          isMutedRef.current = false;
          broadcastMediaState(isCameraOnRef.current, false);
        }).catch((err) => {
          console.error("Failed to capture mic:", err);
        });
      }
    }
  };

  // -------------------------------------------------------------
  // Data Fetching & Sync
  // -------------------------------------------------------------
  const fetchMembersList = async () => {
    try {
      const token = localStorage.getItem("token");
      const response = await fetch(`${API_BASE_URL}/room/${roomCode}/members`, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
          "ngrok-skip-browser-warning": "69420",
        },
      });
      const data = await response.json();
      const memberArray = Array.isArray(data) ? data : [];
      setMembers(memberArray);

      memberArray.forEach((m) => {
        let mId = typeof m === "object" ? m?.userId?.id || m?.userId || m?.id || m?.user?.id : m;
        if (typeof mId === "object" && mId !== null) mId = mId.id || mId.userId;
        let mName = m?.username || m?.name || m?.user?.username || m?.user?.name;

        if (mId && mName) {
          saveStoredRoomName(mId, mName);
        }
      });

      return memberArray;
    } catch (err) {
      console.error("Error fetching members:", err);
      return [];
    }
  };

  const fetchRoom = async () => {
    try {
      const token = localStorage.getItem("token");
      const response = await fetch(`${API_BASE_URL}/room/${roomCode}`, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
          "ngrok-skip-browser-warning": "69420",
        },
      });

      if (!response.ok) throw new Error("Failed to load room details.");
      const data = await response.json();
      setRoom(data);

      const freshMembers = await fetchMembersList();
      fetchMessages(data.id, freshMembers);
    } catch (err) {
      console.error(err);
    }
  };

  const fetchMessages = async (roomId) => {
    if (!roomId) return;
    try {
      const token = localStorage.getItem("token");
      const response = await fetch(`${API_BASE_URL}/chat/${roomId}`, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
          "ngrok-skip-browser-warning": "69420",
        },
      });
      const rawMessages = await response.json();
      const nameMap = getStoredRoomNames();

      const enrichedMessages = (Array.isArray(rawMessages) ? rawMessages : []).map((msg) => {
        let name = msg.username || msg.senderName || msg.sender;
        if (msg.userId && name) {
          saveStoredRoomName(msg.userId, name);
        }
        if (!name || name === "null" || name === "User" || name.startsWith("User #")) {
          name = nameMap[Number(msg.userId)];
        }
        if (Number(msg.userId) === Number(currentUserId)) {
          name = currentUsername;
        }

        return {
          ...msg,
          displayName: name || nameMap[Number(msg.userId)] || `User #${msg.userId}`,
        };
      });

      setMessages(enrichedMessages);
    } catch (err) {
      console.error("Error fetching chat:", err);
    }
  };

  const sendMessage = async () => {
    if (!message.trim() || !room?.id) return;
    try {
      const token = localStorage.getItem("token");
      await fetch(`${API_BASE_URL}/chat/send`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
          "ngrok-skip-browser-warning": "69420",
        },
        body: JSON.stringify({
          roomId: room.id,
          userId: currentUserId,
          username: currentUsername,
          message: message.trim(),
        }),
      });
      setMessage("");
      fetchMessages(room.id);
    } catch (err) {
      console.error("Message send failure:", err);
    }
  };

  const sendBotMessage = async () => {
    if (!botInput.trim() || isBotLoading) return;
    const userText = botInput.trim();
    setBotInput("");

    let currentSeconds = 0.0;
    if (playerRef.current && typeof playerRef.current.getCurrentTime === "function") {
      currentSeconds = playerRef.current.getCurrentTime() || 0.0;
    } else {
      const html5Video = document.getElementById("room-video-player");
      if (html5Video && html5Video.currentTime) {
        currentSeconds = html5Video.currentTime;
      }
    }

    const userMsgObj = {
      id: Date.now(),
      sender: "You",
      text: userText,
      isBot: false,
      timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
    };

    setBotMessages((prev) => [...prev, userMsgObj]);
    setIsBotLoading(true);

    try {
      const token = localStorage.getItem("token");
      const response = await fetch(`${API_BASE_URL}/api/v1/bot/chat`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
          "ngrok-skip-browser-warning": "69420",
        },
        body: JSON.stringify({
          roomId: roomCode || "default-room",
          userMessage: userText,
          currentTimestamp: currentSeconds,
        }),
      });

      if (!response.ok) throw new Error("Bot service offline");
      const data = await response.json();

      const botMsgObj = {
        id: Date.now() + 1,
        sender: "BingeBot",
        text: data.answer || "I couldn't process your question right now.",
        isBot: true,
        timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
      };
      setBotMessages((prev) => [...prev, botMsgObj]);
    } catch (err) {
      console.error("BingeBot Error:", err);
      setBotMessages((prev) => [
        ...prev,
        {
          id: Date.now() + 1,
          sender: "BingeBot",
          text: "Oops! BingeBot ran into a temporary glitch. Try again!",
          isBot: true,
          isError: true,
          timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
        },
      ]);
    } finally {
      setIsBotLoading(false);
    }
  };

  const leaveRoom = async () => {
    leaveMeeting();
    if (!room?.id) return;
    try {
      const token = localStorage.getItem("token");
      await fetch(`${API_BASE_URL}/room/leave`, {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
          "ngrok-skip-browser-warning": "69420",
        },
        body: JSON.stringify({
          roomId: room.id,
          userId: currentUserId,
        }),
      });
      navigate("/");
    } catch (err) {
      console.error("Failed to leave room cleanly:", err);
      navigate("/");
    }
  };

  const isYouTubeUrl = (url) => {
    if (!url) return false;
    return url.includes("youtube.com") || url.includes("youtu.be");
  };

  const getYouTubeId = (url) => {
    if (!url) return "";
    const regex = /(?:youtube\.com.*v=|youtu\.be\/|youtube\.com\/embed\/)([^&?/\s]+)/;
    const match = url.match(regex);
    return match ? match[1] : url;
  };

  const formatTime = (timeInSeconds) => {
    if (isNaN(timeInSeconds)) return "0:00";
    const minutes = Math.floor(timeInSeconds / 60);
    const seconds = Math.floor(timeInSeconds % 60);
    return `${minutes}:${seconds < 10 ? "0" : ""}${seconds}`;
  };

  const handleCopyCode = () => {
    if (roomCode) {
      navigator.clipboard.writeText(roomCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  useEffect(() => {
    if (!roomCode) return;
    fetchRoom();
  }, [roomCode]);

  useEffect(() => {
    if (!room?.id) return;
    const interval = setInterval(() => {
      fetchMessages(room.id);
      fetchMembersList();
    }, 3000);
    return () => clearInterval(interval);
  }, [room?.id]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  useEffect(() => {
    scrollBotToBottom();
  }, [botMessages, isBotLoading, activeTab]);

  // -------------------------------------------------------------
  // Simultaneous Video Player Synchronization
  // -------------------------------------------------------------
  const handleLocalPlay = () => {
    if (ignoreNextSyncRef.current) return;
    stompClientRef.current?.publish({
      destination: `/app/room/${roomCode}/sync`,
      body: JSON.stringify({
        sender: currentUsername,
        userId: currentUserId,
        action: "PLAY",
      }),
    });
  };

  const handleLocalPause = () => {
    if (ignoreNextSyncRef.current) return;
    stompClientRef.current?.publish({
      destination: `/app/room/${roomCode}/sync`,
      body: JSON.stringify({
        sender: currentUsername,
        userId: currentUserId,
        action: "PAUSE",
      }),
    });
  };

  const handleLocalSeek = (seconds) => {
    const client = stompClientRef.current;
    if (client && client.connected) {
      client.publish({
        destination: `/app/room/${roomCode}/sync`,
        body: JSON.stringify({
          sender: currentUsername,
          userId: currentUserId,
          action: "SEEK_REQUEST",
          targetTime: seconds,
        }),
      });
    }
  };

  const handleApplySync = (targetTime) => {
    const activeSource = demoVideo || room?.movieLink;
    if (isYouTubeUrl(activeSource)) {
      if (playerRef.current && typeof playerRef.current.seekTo === "function") {
        isSeekingRef.current = true;
        ignoreNextSyncRef.current = true;
        playerRef.current.seekTo(targetTime, true);
        setTimeout(() => {
          isSeekingRef.current = false;
        }, 1200);
      }
    } else {
      if (playerRef.current && typeof playerRef.current.seekTo === "function") {
        playerRef.current.seekTo(targetTime);
      } else {
        const html5Player = document.getElementById("room-video-player");
        if (html5Player) {
          html5Player.currentTime = targetTime;
        }
      }
    }
    setPendingSync(null);
  };

  // YouTube API Initialization
  useEffect(() => {
    if (!room?.movieLink || !isYouTubeUrl(room.movieLink)) return;

    if (!window.YT) {
      const tag = document.createElement("script");
      tag.src = "https://www.youtube.com/iframe_api";
      const firstScriptTag = document.getElementsByTagName("script")[0];
      firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
    }

    const initPlayer = () => {
      playerRef.current = new window.YT.Player("room-video-player", {
        events: {
          onStateChange: (event) => {
            if (ignoreNextSyncRef.current) {
              if (
                event.data === window.YT.PlayerState.PLAYING ||
                event.data === window.YT.PlayerState.PAUSED
              ) {
                ignoreNextSyncRef.current = false;
              }
              return;
            }

            if (event.data === window.YT.PlayerState.PLAYING) {
              handleLocalPlay();
            } else if (event.data === window.YT.PlayerState.PAUSED) {
              handleLocalPause();
            } else if (event.data === window.YT.PlayerState.BUFFERING && !isSeekingRef.current) {
              setTimeout(() => {
                if (playerRef.current && typeof playerRef.current.getCurrentTime === "function") {
                  handleLocalSeek(playerRef.current.getCurrentTime());
                }
              }, 250);
            }
          },
        },
      });
    };

    if (window.YT && window.YT.Player) {
      initPlayer();
    } else {
      window.onYouTubeIframeAPIReady = initPlayer;
    }

    return () => {
      if (playerRef.current?.destroy) {
        playerRef.current.destroy();
      }
    };
  }, [room?.movieLink]);

  // -------------------------------------------------------------
  // STOMP WebSocket & WebRTC Signals Listener
  // CRITICAL: Effect ONLY depends on roomCode, currentUserId, currentUsername.
  // It does NOT depend on camera/mic states, so it NEVER tears down on toggle!
  // -------------------------------------------------------------
  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(WS_BASE_URL),
      connectHeaders: {
        "ngrok-skip-browser-warning": "true",
      },
      reconnectDelay: 5000,
      onConnect: () => {
        // Announce presence in room
        client.publish({
          destination: `/app/room/${roomCode}/sync`,
          body: JSON.stringify({
            sender: currentUsername,
            userId: currentUserId,
            action: "ANNOUNCE",
          }),
        });

        // 1. Media Player Playback Sync & Meeting Presence Listener
        client.subscribe(`/topic/room/${roomCode}/stream`, (message) => {
          const payload = JSON.parse(message.body);
          const packetSender = payload.sender || payload.username || payload.nickname;
          const packetUserId = Number(payload.userId);

          if (packetUserId && packetSender) {
            saveStoredRoomName(packetUserId, packetSender);
          }

          if (packetUserId === currentUserId) return;

          // Simultaneous Media Playback Sync
          if (payload.action === "PLAY") {
            ignoreNextSyncRef.current = true;
            playerRef.current?.play?.();
          } else if (payload.action === "PAUSE") {
            ignoreNextSyncRef.current = true;
            playerRef.current?.pause?.();
          } else if (payload.action === "SEEK_REQUEST" && payload.targetTime !== undefined) {
            setPendingSync({
              sender: packetSender || "Someone",
              targetTime: Number(payload.targetTime),
            });
          }

          // Meeting Presence & Media State Handling
          if (payload.action === "MEETING_JOINED" || payload.action === "ANNOUNCE") {
            if (isInCallRef.current) {
              callPeer(packetUserId);
              broadcastMediaState(isCameraOnRef.current, isMutedRef.current);
            }
          } else if (payload.action === "MEDIA_STATE_UPDATE") {
            setPeerMediaStates((prev) => ({
              ...prev,
              [packetUserId]: {
                isCameraOn: !!payload.isCameraOn,
                isMuted: !!payload.isMuted,
                name: packetSender,
              },
            }));
          } else if (payload.action === "MEETING_LEFT") {
            if (peerConnectionsRef.current[packetUserId]) {
              peerConnectionsRef.current[packetUserId].close();
              delete peerConnectionsRef.current[packetUserId];
            }
            delete pendingCandidatesRef.current[packetUserId];
            setRemoteStreams((prev) => {
              const updated = { ...prev };
              delete updated[packetUserId];
              return updated;
            });
            setPeerMediaStates((prev) => {
              const updated = { ...prev };
              delete updated[packetUserId];
              return updated;
            });
          }
        });

        // 2. WebRTC Offer Receiver (Person B receives Person A's camera feed!)
        client.subscribe(`/topic/room/${roomCode}/webrtc/offer`, async (msg) => {
          const data = JSON.parse(msg.body);
          if (Number(data.targetId) !== currentUserId) return;

          try {
            const pc = getOrCreatePeerConnection(data.senderId);
            await pc.setRemoteDescription(new RTCSessionDescription(data.offer));
            await drainIceCandidates(data.senderId, pc);

            const answer = await pc.createAnswer();
            await pc.setLocalDescription(answer);

            client.publish({
              destination: `/app/room/${roomCode}/webrtc/answer`,
              body: JSON.stringify({
                senderId: currentUserId,
                targetId: data.senderId,
                answer: answer,
              }),
            });
          } catch (err) {
            console.error("Failed to handle WebRTC offer:", err);
          }
        });

        // 3. WebRTC Answer Receiver
        client.subscribe(`/topic/room/${roomCode}/webrtc/answer`, async (msg) => {
          const data = JSON.parse(msg.body);
          if (Number(data.targetId) !== currentUserId) return;

          const pc = peerConnectionsRef.current[data.senderId];
          if (pc) {
            try {
              await pc.setRemoteDescription(new RTCSessionDescription(data.answer));
              await drainIceCandidates(data.senderId, pc);
            } catch (err) {
              console.error("Failed to set remote description on answer:", err);
            }
          }
        });

        // 4. WebRTC ICE Candidate Receiver with Queueing
        client.subscribe(`/topic/room/${roomCode}/webrtc/candidate`, async (msg) => {
          const data = JSON.parse(msg.body);
          if (Number(data.targetId) !== currentUserId) return;

          const pc = peerConnectionsRef.current[data.senderId];
          if (pc && pc.remoteDescription && pc.remoteDescription.type) {
            try {
              await pc.addIceCandidate(new RTCIceCandidate(data.candidate));
            } catch (e) {
              console.error("Error adding direct ICE candidate:", e);
            }
          } else {
            if (!pendingCandidatesRef.current[data.senderId]) {
              pendingCandidatesRef.current[data.senderId] = [];
            }
            pendingCandidatesRef.current[data.senderId].push(data.candidate);
          }
        });
      },
    });

    client.activate();
    stompClientRef.current = client;

    return () => {
      leaveMeeting();
      if (stompClientRef.current) stompClientRef.current.deactivate();
    };
  }, [roomCode, currentUserId, currentUsername]);

  const resolveMemberName = (m, idx) => {
    if (!m) return idx === 0 ? currentUsername : `Member #${idx + 1}`;

    let mUserId = null;
    if (typeof m === "number" || typeof m === "string") {
      mUserId = m;
    } else if (m && typeof m === "object") {
      mUserId = m.userId?.id || m.userId || m.id || m.user?.id;
      if (typeof mUserId === "object" && mUserId !== null) {
        mUserId = mUserId.id || mUserId.userId;
      }
    }

    if (mUserId && Number(mUserId) === Number(currentUserId)) {
      return currentUsername;
    }

    const storedMap = getStoredRoomNames();
    if (mUserId && storedMap[Number(mUserId)]) {
      return storedMap[Number(mUserId)];
    }

    let rawName = null;
    if (typeof m === "string") {
      rawName = m;
    } else if (m && typeof m === "object") {
      rawName =
        m.username ||
        m.name ||
        m.nickname ||
        m.user?.username ||
        m.user?.name ||
        (m.email ? m.email.split("@")[0] : null);
    }

    if (
      rawName &&
      rawName !== "null" &&
      rawName !== "User" &&
      !rawName.startsWith("User #") &&
      !rawName.startsWith("Member #")
    ) {
      return rawName;
    }

    return mUserId ? `User #${mUserId}` : `Member #${idx + 1}`;
  };

  const allParticipantIds = Array.from(
    new Set([
      ...Object.keys(remoteStreams).map(Number),
      ...Object.keys(peerMediaStates).map(Number),
    ])
  ).filter((id) => id !== currentUserId);

  return (
    <div className="room-container">
      {/* CENTERED POPUP MODAL FOR SYNC */}
      {pendingSync && (
        <div className="sync-modal-backdrop">
          <div className="sync-modal-card">
            <button className="sync-close-x" onClick={() => setPendingSync(null)}>
              ✕
            </button>
            <div className="sync-icon">🎬</div>
            <h3>
              <strong>{pendingSync.sender}</strong> wants to skip to{" "}
              <strong>{formatTime(pendingSync.targetTime)}</strong>
            </h3>
            <p className="sync-subtext">Everyone will be synced in real-time</p>

            <div className="sync-btn-group">
              <button
                className="sync-accept-btn"
                onClick={() => handleApplySync(pendingSync.targetTime)}
              >
                Accept
              </button>
              <button className="sync-ignore-btn" onClick={() => setPendingSync(null)}>
                Ignore
              </button>
            </div>
          </div>
        </div>
      )}

      {/* TOP HEADER NAV BAR */}
      <header className="room-navbar">
        <div className="nav-left-group">
          <span className="room-logo-icon">🎬</span>
          <h1 className="room-main-title">{room?.roomName || "Watch Party"}</h1>
          <span className="room-badge">{room?.roomType || "Solo"}</span>

          <div className="room-code-chip">
            <span className="code-lbl">
              Room Code: <strong>{roomCode}</strong>
            </span>
            <button className="copy-code-btn" onClick={handleCopyCode}>
              {copied ? "✓ Copied" : "📋 Copy"}
            </button>
          </div>
        </div>

        <div className="nav-right-group">
          <div className="avatar-stack">
            {members.length > 0 ? (
              members.slice(0, 3).map((m, idx) => {
                const displayName = resolveMemberName(m, idx);
                return (
                  <div key={idx} className="stack-avatar" title={displayName}>
                    {displayName.charAt(0).toUpperCase()}
                  </div>
                );
              })
            ) : (
              <div className="stack-avatar" title={currentUsername}>
                {currentUsername.charAt(0).toUpperCase()}
              </div>
            )}

            {members.length > 3 && (
              <div className="stack-avatar extra">+{members.length - 3}</div>
            )}
          </div>

          <button className="leave-room-header-btn" onClick={leaveRoom}>
            Leave Room
          </button>
        </div>
      </header>

      {/* MAIN WATCH PARTY LAYOUT */}
      <div className="room-content-layout">
        <div className="left-stage-column">
          {/* 1. MEDIA VIDEO PLAYER (Plays concurrently with video call!) */}
          <div className="video-player-frame">
            {demoVideo || room?.movieLink ? (
              isYouTubeUrl(demoVideo || room?.movieLink) ? (
                <iframe
                  id="room-video-player"
                  width="100%"
                  height="460"
                  src={`https://www.youtube.com/embed/${getYouTubeId(
                    demoVideo || room.movieLink
                  )}?enablejsapi=1&origin=${window.location.origin}`}
                  title="YouTube Video"
                  frameBorder="0"
                  allow="autoplay; encrypted-media"
                  allowFullScreen
                />
              ) : (
                <CustomVideoPlayer
                  ref={playerRef}
                  src={demoVideo || room.movieLink}
                  title={room?.roomName || "Watch Party Stream"}
                  onPlay={handleLocalPlay}
                  onPause={handleLocalPause}
                  onSeeked={(time) => handleLocalSeek(time)}
                />
              )
            ) : (
              <div className="no-video-placeholder">
                <span>🍿</span>
                <p>No video source attached to this room.</p>
                <div
                  style={{
                    marginTop: "16px",
                    display: "flex",
                    gap: "10px",
                    flexWrap: "wrap",
                    justifyContent: "center",
                  }}
                >
                  <button
                    className="modal-btn secondary"
                    style={{ fontSize: "12px", padding: "8px 14px", cursor: "pointer" }}
                    onClick={() =>
                      setDemoVideo(
                        "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
                      )
                    }
                  >
                    🎬 Load Multi-Audio/Subtitles Demo
                  </button>
                  <button
                    className="modal-btn secondary"
                    style={{ fontSize: "12px", padding: "8px 14px", cursor: "pointer" }}
                    onClick={() =>
                      setDemoVideo("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")
                    }
                  >
                    🍿 Load Big Buck Bunny HLS
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* 2. PARTY CAM & VOICE CHAT DOCK */}
          <div className="party-cam-stage">
            <div className="party-cam-header">
              <div className="party-header-title">
                <span className="live-pulsing-dot"></span>
                <h3>Party Cam & Voice</h3>
                <span className="party-cam-tag">Live</span>
              </div>

              {/* Controls: Independent Mic, Camera, & Leave Call */}
              <div className="party-controls-bar">
                {!isInCall ? (
                  <div className="join-options-group">
                    <button className="party-btn primary-join" onClick={joinMeeting}>
                      🎙️ Join Voice Only
                    </button>
                    <button className="party-btn camera-join" onClick={toggleCamera}>
                      🎥 Turn Camera On
                    </button>
                  </div>
                ) : (
                  <>
                    <button
                      className={`party-btn ${isMuted ? "btn-danger" : "btn-neutral"}`}
                      onClick={toggleMute}
                      title={isMuted ? "Unmute Microphone" : "Mute Microphone"}
                    >
                      {isMuted ? "🔇 Unmute Mic" : "🎙️ Mute Mic"}
                    </button>

                    <button
                      className={`party-btn ${isCameraOn ? "btn-active" : "btn-neutral"}`}
                      onClick={toggleCamera}
                      title={isCameraOn ? "Turn Camera Off" : "Turn Camera On"}
                    >
                      {isCameraOn ? "📷 Camera On" : "🎥 Camera Off"}
                    </button>

                    <button
                      className="party-btn btn-leave"
                      onClick={leaveMeeting}
                      title="Disconnect Call"
                    >
                      📞 Disconnect
                    </button>
                  </>
                )}
              </div>
            </div>

            {/* Participant Video / Avatar Grid */}
            <div className="party-tiles-grid">
              {/* Local Participant Tile */}
              {isInCall && (
                <ParticipantVideoTile
                  stream={localStream}
                  name={currentUsername}
                  isLocal={true}
                  isCameraOn={isCameraOn}
                  isMuted={isMuted}
                  avatarInitial={currentUsername.charAt(0).toUpperCase()}
                />
              )}

              {/* Remote Participants Tiles */}
              {allParticipantIds.map((peerId) => {
                const stream = remoteStreams[peerId];
                const state = peerMediaStates[peerId] || {};
                const name =
                  state.name || getStoredRoomNames()[peerId] || `User #${peerId}`;
                const hasVideoTrack =
                  stream &&
                  stream.getVideoTracks().some((t) => t.enabled && t.readyState === "live");
                const remoteCameraActive = state.isCameraOn ?? hasVideoTrack;

                return (
                  <ParticipantVideoTile
                    key={peerId}
                    stream={stream}
                    name={name}
                    isLocal={false}
                    isCameraOn={remoteCameraActive}
                    isMuted={!!state.isMuted}
                    avatarInitial={name.charAt(0).toUpperCase()}
                  />
                );
              })}

              {!isInCall && allParticipantIds.length === 0 && (
                <div className="empty-party-placeholder">
                  <span className="party-icon-large">📹</span>
                  <p>Turn on your camera or mic to chat live with your watch party!</p>
                </div>
              )}
            </div>
          </div>

          {demoVideo && (
            <div
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                background: "rgba(147, 51, 234, 0.15)",
                border: "1px solid rgba(168, 85, 247, 0.3)",
                borderRadius: "10px",
                padding: "8px 14px",
                fontSize: "12px",
                color: "#e2e8f0",
                marginTop: "10px",
              }}
            >
              <span>
                ✨ <b>Demo Stream Active:</b> Tears of Steel HLS
              </span>
              <button
                style={{
                  background: "transparent",
                  border: "none",
                  color: "#f472b6",
                  cursor: "pointer",
                  fontWeight: "bold",
                }}
                onClick={() => setDemoVideo(null)}
              >
                ✕ Reset
              </button>
            </div>
          )}

          {/* MEMBERS PANEL */}
          <div className="members-panel">
            <h3 className="members-title">Members ({members.length || 1})</h3>
            <div className="members-chips-grid">
              {members.length > 0 ? (
                members.map((member, i) => {
                  const displayName = resolveMemberName(member, i);
                  const isHost = i === 0;

                  return (
                    <div key={i} className="member-card-chip">
                      <div className="member-avatar">
                        {displayName.charAt(0).toUpperCase()}
                      </div>
                      <span className="member-name-text">
                        {displayName} {isHost && <span className="host-tag">(Host)</span>}
                      </span>
                      <span className="online-indicator-dot"></span>
                    </div>
                  );
                })
              ) : (
                <div className="member-card-chip">
                  <div className="member-avatar">
                    {currentUsername.charAt(0).toUpperCase()}
                  </div>
                  <span className="member-name-text">
                    {currentUsername} <span className="host-tag">(Host)</span>
                  </span>
                  <span className="online-indicator-dot"></span>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* CHAT SECTION WITH TABS */}
        <div className="right-chat-column">
          <div className="chat-panel-header-tabs">
            <button
              className={`chat-tab-btn ${activeTab === "chat" ? "active" : ""}`}
              onClick={() => setActiveTab("chat")}
            >
              💬 Room Chat
            </button>
            <button
              className={`chat-tab-btn ${activeTab === "bot" ? "active" : ""}`}
              onClick={() => setActiveTab("bot")}
            >
              🤖 BingeBot <span className="tab-badge">AI</span>
            </button>
          </div>

          {/* TAB 1: GROUP CHAT */}
          {activeTab === "chat" && (
            <>
              <div className="chat-messages-scroll">
                {Array.isArray(messages) && messages.length > 0 ? (
                  messages.map((msg, index) => {
                    const isMyMessage = Number(msg.userId) === currentUserId;
                    const senderName = isMyMessage
                      ? "You"
                      : msg.displayName || currentUsername;

                    return (
                      <div
                        key={msg.id || index}
                        className={`message-row ${
                          isMyMessage ? "own-row" : "other-row"
                        }`}
                      >
                        {!isMyMessage && (
                          <div className="msg-avatar">
                            {senderName.charAt(0).toUpperCase()}
                          </div>
                        )}

                        <div className="msg-content-wrapper">
                          <div className="msg-header-info">
                            <span className="msg-author">{senderName}</span>
                          </div>

                          <div
                            className={`msg-bubble ${
                              isMyMessage ? "own-bubble" : "other-bubble"
                            }`}
                          >
                            <p>{msg.message}</p>
                          </div>
                        </div>
                      </div>
                    );
                  })
                ) : (
                  <div className="empty-chat-msg">
                    <span>💬</span>
                    <p>No messages yet. Say hello to the room!</p>
                  </div>
                )}
                <div ref={messagesEndRef}></div>
              </div>

              <div className="chat-input-bar">
                <input
                  type="text"
                  placeholder="Type a message..."
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && sendMessage()}
                />
                <button className="chat-send-btn" onClick={sendMessage}>
                  Send
                </button>
              </div>
            </>
          )}

          {/* TAB 2: PRIVATE BINGEBOT AI */}
          {activeTab === "bot" && (
            <>
              <div className="chat-messages-scroll">
                {botMessages.map((msg) => (
                  <div
                    key={msg.id}
                    className={`message-row ${
                      msg.isBot ? "other-row" : "own-row"
                    }`}
                  >
                    {msg.isBot && <div className="msg-avatar bot-avatar-icon">🤖</div>}

                    <div className="msg-content-wrapper">
                      <div className="msg-header-info">
                        <span className="msg-author">{msg.sender}</span>
                        <span className="msg-time">{msg.timestamp}</span>
                      </div>

                      <div
                        className={`msg-bubble ${
                          msg.isBot ? "other-bubble bot-bubble" : "own-bubble"
                        } ${msg.isError ? "error-bubble" : ""}`}
                      >
                        <p>{msg.text}</p>
                      </div>
                    </div>
                  </div>
                ))}

                {isBotLoading && (
                  <div className="message-row other-row">
                    <div className="msg-avatar bot-avatar-icon">🤖</div>
                    <div className="msg-content-wrapper">
                      <div className="msg-header-info">
                        <span className="msg-author">BingeBot</span>
                      </div>
                      <div className="msg-bubble other-bubble bot-bubble loading-dots">
                        <span>Thinking...</span>
                      </div>
                    </div>
                  </div>
                )}
                <div ref={botMessagesEndRef}></div>
              </div>

              <div className="chat-input-bar">
                <input
                  type="text"
                  placeholder="Ask BingeBot anything..."
                  value={botInput}
                  onChange={(e) => setBotInput(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && sendBotMessage()}
                  disabled={isBotLoading}
                />
                <button
                  className="chat-send-btn bot-send-btn"
                  onClick={sendBotMessage}
                  disabled={!botInput.trim() || isBotLoading}
                >
                  Ask
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

export default RoomPage;
```

---

### File 2: `src/pages/RoomPage.css`

Replace the entire contents of **`src/pages/RoomPage.css`** with the following:

```css
/* =========================================================
   BINGETOGETHER WATCH ROOM STYLES (PARTY CAM & VOICE)
   ========================================================= */

.room-container {
  min-height: 100vh;
  width: 100%;
  background: #09090f;
  color: #ffffff;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
  padding: 20px 24px 40px 24px;
  box-sizing: border-box;
}

/* 1. TOP NAVBAR */
.room-navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(18, 18, 28, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 12px 20px;
  margin-bottom: 20px;
}

.nav-left-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.room-logo-icon {
  font-size: 24px;
}

.room-main-title {
  font-size: 20px;
  font-weight: 800;
  margin: 0;
  color: #ffffff;
  letter-spacing: -0.3px;
}

.room-badge {
  background: rgba(124, 93, 250, 0.15);
  color: #7c5dfa;
  border: 1px solid rgba(124, 93, 250, 0.3);
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 6px;
  font-weight: 600;
  text-transform: capitalize;
}

.room-code-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #10101a;
  border: 1px solid #28283d;
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 12px;
  color: #a0a0c0;
  margin-left: 8px;
}

.room-code-chip strong {
  color: #ffffff;
}

.copy-code-btn {
  background: rgba(255, 255, 255, 0.08);
  border: none;
  color: #ffffff;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 600;
  transition: background 0.2s;
}

.copy-code-btn:hover {
  background: rgba(124, 93, 250, 0.4);
}

.nav-right-group {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-stack {
  display: flex;
  align-items: center;
}

.stack-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #7c5dfa, #4831d4);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  border: 2px solid #181826;
  margin-left: -8px;
}

.stack-avatar:first-child {
  margin-left: 0;
}

.stack-avatar.extra {
  background: #28283d;
  font-size: 10px;
}

.leave-room-header-btn {
  background: rgba(255, 71, 87, 0.12);
  border: 1px solid rgba(255, 71, 87, 0.4);
  color: #ff4757;
  padding: 8px 16px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.leave-room-header-btn:hover {
  background: #ff4757;
  color: #ffffff;
}

/* 2. MAIN LAYOUT GRID */
.room-content-layout {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 20px;
}

@media (max-width: 950px) {
  .room-content-layout {
    grid-template-columns: 1fr;
  }
}

/* LEFT COLUMN */
.left-stage-column {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.video-player-frame {
  background: #000000;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.6);
}

.no-video-placeholder {
  height: 400px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #8888a0;
}

.no-video-placeholder span {
  font-size: 48px;
  margin-bottom: 8px;
}

/* =========================================================
   3. PARTY CAM & VOICE STAGE
   ========================================================= */

.party-cam-stage {
  background: rgba(18, 18, 28, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.4);
}

.party-cam-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  padding-bottom: 12px;
}

.party-header-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.party-header-title h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #ffffff;
}

.live-pulsing-dot {
  width: 8px;
  height: 8px;
  background: #2ed573;
  border-radius: 50%;
  box-shadow: 0 0 8px #2ed573;
  animation: pulseLive 2s infinite;
}

@keyframes pulseLive {
  0% { transform: scale(0.95); opacity: 0.8; }
  50% { transform: scale(1.3); opacity: 1; }
  100% { transform: scale(0.95); opacity: 0.8; }
}

.party-cam-tag {
  background: rgba(46, 213, 115, 0.15);
  color: #2ed573;
  border: 1px solid rgba(46, 213, 115, 0.3);
  font-size: 10.5px;
  padding: 2px 7px;
  border-radius: 5px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* Control Buttons */
.party-controls-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.party-btn {
  border: none;
  padding: 8px 14px;
  border-radius: 10px;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}

.party-btn.primary-join {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: #ffffff;
}

.party-btn.primary-join:hover {
  background: rgba(255, 255, 255, 0.14);
}

.party-btn.camera-join {
  background: linear-gradient(135deg, #7c5dfa, #5b36f5);
  color: #ffffff;
  box-shadow: 0 4px 14px rgba(124, 93, 250, 0.3);
}

.party-btn.camera-join:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(124, 93, 250, 0.45);
}

.party-btn.btn-neutral {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #ffffff;
}

.party-btn.btn-neutral:hover {
  background: rgba(255, 255, 255, 0.14);
}

.party-btn.btn-active {
  background: rgba(46, 213, 115, 0.15);
  border: 1px solid rgba(46, 213, 115, 0.4);
  color: #2ed573;
}

.party-btn.btn-danger {
  background: rgba(239, 68, 68, 0.15);
  border: 1px solid rgba(239, 68, 68, 0.4);
  color: #fca5a5;
}

.party-btn.btn-leave {
  background: rgba(239, 68, 68, 0.12);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: #ef4444;
}

.party-btn.btn-leave:hover {
  background: #ef4444;
  color: #ffffff;
}

/* 4. PARTICIPANT TILES GRID */
.party-tiles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  min-height: 120px;
}

.empty-party-placeholder {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  color: #71717a;
  text-align: center;
  font-size: 13px;
}

.party-icon-large {
  font-size: 32px;
  margin-bottom: 6px;
  opacity: 0.6;
}

.participant-tile {
  position: relative;
  aspect-ratio: 16 / 9;
  min-height: 120px;
  background: #12121e;
  border: 1.5px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.5);
  transition: border-color 0.2s ease;
}

.participant-tile.local-participant {
  border-color: rgba(124, 93, 250, 0.4);
}

.participant-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* Only the local user's own preview is mirrored */
.participant-video.mirrored {
  transform: scaleX(-1);
}

/* Camera-Off Avatar */
.avatar-placeholder-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle, #1a1a2b 0%, #0d0d17 100%);
}

.party-avatar-circle {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: linear-gradient(135deg, #7c5dfa, #4831d4);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  box-shadow: 0 4px 15px rgba(124, 93, 250, 0.35);
}

.camera-off-indicator {
  font-size: 10px;
  color: #9ca3af;
  background: rgba(0, 0, 0, 0.4);
  padding: 2px 7px;
  border-radius: 4px;
}

/* Tile Bottom Name & Mic Bar */
.tile-bottom-bar {
  position: absolute;
  bottom: 6px;
  left: 8px;
  right: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  pointer-events: none;
}

.participant-name {
  background: rgba(0, 0, 0, 0.65);
  backdrop-filter: blur(6px);
  color: #f3f4f6;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 6px;
  max-width: 120px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mic-badge {
  background: rgba(0, 0, 0, 0.65);
  backdrop-filter: blur(6px);
  padding: 2px 6px;
  border-radius: 6px;
  font-size: 11px;
}

.mic-badge.muted {
  color: #ef4444;
}

.mic-badge.unmuted {
  color: #2ed573;
}

/* 5. MEMBERS PANEL */
.members-panel {
  background: rgba(18, 18, 28, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 16px;
  padding: 18px;
  text-align: left;
}

.members-title {
  margin: 0 0 14px 0;
  font-size: 15px;
  font-weight: 700;
  color: #ffffff;
}

.members-chips-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.member-card-chip {
  background: #10101a;
  border: 1px solid #28283d;
  padding: 8px 14px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.member-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #4831d4;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
}

.member-name-text {
  font-size: 13px;
  font-weight: 600;
  color: #e0e0f0;
}

.host-tag {
  color: #7c5dfa;
  font-size: 11px;
}

.online-indicator-dot {
  width: 7px;
  height: 7px;
  background: #2ed573;
  border-radius: 50%;
  box-shadow: 0 0 6px #2ed573;
}

/* 6. RIGHT COLUMN: CHAT */
.right-chat-column {
  background: rgba(18, 18, 28, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 18px;
  display: flex;
  flex-direction: column;
  height: 580px;
  overflow: hidden;
}

.chat-panel-header-tabs {
  display: flex;
  background-color: #1a1a24;
  border-bottom: 1px solid #2e2e3e;
  padding: 4px;
}

.chat-tab-btn {
  flex: 1;
  padding: 10px 12px;
  background: transparent;
  border: none;
  color: #a0a0b0;
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.chat-tab-btn.active {
  background-color: #2a2a3c;
  color: #ffffff;
}

.tab-badge {
  font-size: 0.65rem;
  background-color: #6366f1;
  color: #fff;
  padding: 2px 6px;
  border-radius: 4px;
}

.chat-messages-scroll {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty-chat-msg {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #7a7a9a;
  font-size: 13px;
}

.empty-chat-msg span {
  font-size: 32px;
  margin-bottom: 6px;
}

.message-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  max-width: 85%;
}

.own-row {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.other-row {
  align-self: flex-start;
}

.msg-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #3a3a52;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: bold;
  flex-shrink: 0;
}

.msg-content-wrapper {
  display: flex;
  flex-direction: column;
}

.own-row .msg-content-wrapper {
  align-items: flex-end;
}

.msg-header-info {
  margin-bottom: 3px;
}

.msg-author {
  font-size: 11px;
  color: #8888a0;
  font-weight: 600;
}

.msg-bubble {
  padding: 10px 14px;
  border-radius: 14px;
  word-break: break-word;
  text-align: left;
}

.own-bubble {
  background: linear-gradient(135deg, #4831d4, #6543f8);
  color: #ffffff;
  border-bottom-right-radius: 2px;
}

.other-bubble {
  background: #181826;
  border: 1px solid #28283d;
  color: #e0e0f0;
  border-bottom-left-radius: 2px;
}

.msg-bubble p {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.4;
}

.bot-avatar-icon {
  background: #6366f1 !important;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
}

.bot-bubble {
  border-left: 3px solid #6366f1 !important;
}

.error-bubble {
  border-left: 3px solid #ef4444 !important;
  background-color: #3f1d1d !important;
}

.loading-dots span {
  font-style: italic;
  opacity: 0.8;
}

/* CHAT INPUT BAR */
.chat-input-bar {
  padding: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  gap: 10px;
  background: #10101a;
}

.chat-input-bar input {
  flex: 1;
  background: #181826;
  border: 1px solid #28283d;
  border-radius: 10px;
  padding: 10px 14px;
  color: #ffffff;
  font-size: 13px;
  outline: none;
}

.chat-input-bar input:focus {
  border-color: #7c5dfa;
}

.chat-send-btn {
  background: #7c5dfa;
  color: #ffffff;
  border: none;
  border-radius: 10px;
  padding: 10px 16px;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}

.chat-send-btn:hover {
  background: #6543f8;
}

.bot-send-btn {
  background-color: #6366f1 !important;
}

/* CENTERED POPUP MODAL FOR SYNC */
.sync-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(8px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.sync-modal-card {
  background: #161624;
  border: 1px solid rgba(124, 93, 250, 0.4);
  border-radius: 20px;
  padding: 28px;
  max-width: 380px;
  width: 100%;
  text-align: center;
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.8);
  position: relative;
  animation: modalPop 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modalPop {
  from { opacity: 0; transform: scale(0.92); }
  to { opacity: 1; transform: scale(1); }
}

.sync-close-x {
  position: absolute;
  right: 16px;
  top: 16px;
  background: none;
  border: none;
  color: #7a7a9a;
  cursor: pointer;
  font-size: 16px;
}

.sync-icon {
  font-size: 36px;
  margin-bottom: 8px;
}

.sync-modal-card h3 {
  margin: 0 0 6px 0;
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
}

.sync-subtext {
  color: #8888a0;
  font-size: 12px;
  margin: 0 0 20px 0;
}

.sync-btn-group {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.sync-accept-btn {
  background: #7c5dfa;
  color: #ffffff;
  border: none;
  padding: 10px;
  border-radius: 10px;
  font-weight: 700;
  font-size: 13.5px;
  cursor: pointer;
  transition: background 0.2s;
}

.sync-accept-btn:hover {
  background: #6543f8;
}

.sync-ignore-btn {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #ffffff;
  padding: 10px;
  border-radius: 10px;
  font-weight: 600;
  font-size: 13.5px;
  cursor: pointer;
}

.sync-ignore-btn:hover {
  background: rgba(255, 255, 255, 0.12);
}

@media (max-width: 900px) {
  .room-container {
    padding: 12px;
  }

  .room-navbar {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .nav-right-group {
    width: 100%;
    justify-content: space-between;
  }

  .room-content-layout {
    grid-template-columns: 1fr;
  }

  .right-chat-column {
    height: 450px;
  }

  #room-video-player {
    height: 220px !important;
  }
}
```

---

## 5. Testing & Verification

1. Start your development server:
   ```bash
   npm run dev
   ```
2. Open a room (`/room/<roomCode>`).
3. Click **"🎥 Turn Camera On"**:
   - Camera light will turn on.
   - **Verification**: Notice that the camera stays ON stably! It will **NOT** shut off by itself because the STOMP WebSocket connection is no longer torn down.
4. Click **"🔇 Mute Mic"** / **"🎙️ Unmute Mic"**:
   - Mic toggles smoothly without touching the camera or disconnecting the room.
5. Notice all "Google Meet" labels have been completely removed and replaced with **Party Cam & Voice**.
