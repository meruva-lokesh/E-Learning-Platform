/** Shapes of the video API (see VideoController). */

export interface VideoProgress {
  positionSec: number;
  percent: number;
  completed: boolean;
}

export interface CourseVideo {
  id: number;
  courseId: number;
  title: string;
  description: string | null;
  sizeBytes: number;
  durationSec: number;
  uploadedAtMs: number;
  progress?: VideoProgress | null;
}

export interface VideoTicket {
  ticket: string;
  url: string;
  expiresInSeconds: number;
}

export interface VideoStat {
  videoId: number;
  title: string;
  durationSec: number;
  viewers: number;
  completedCount: number;
  averagePercent: number;
}

export interface CourseVideoStats {
  courseId: number;
  enrolledCustomers: number;
  videos: VideoStat[];
}

export interface LearningItem {
  courseId: number;
  courseType: string;
  totalVideos: number;
  completedVideos: number;
  percent: number;
  resumeVideoId: number | null;
  resumePositionSec: number;
}

/** "75" -> "1:15", "3725" -> "1:02:05", 0 -> "--". */
export function formatDuration(seconds: number): string {
  if (!seconds || seconds < 0) { return '--'; }
  const s = Math.floor(seconds % 60);
  const m = Math.floor((seconds / 60) % 60);
  const h = Math.floor(seconds / 3600);
  const two = (n: number) => (n < 10 ? '0' + n : String(n));
  return h > 0 ? `${h}:${two(m)}:${two(s)}` : `${m}:${two(s)}`;
}

/** 1536 -> "1.5 KB", 5242880 -> "5.0 MB". */
export function formatSize(bytes: number): string {
  if (bytes < 1024) { return bytes + ' B'; }
  if (bytes < 1024 * 1024) { return (bytes / 1024).toFixed(1) + ' KB'; }
  if (bytes < 1024 * 1024 * 1024) { return (bytes / 1024 / 1024).toFixed(1) + ' MB'; }
  return (bytes / 1024 / 1024 / 1024).toFixed(2) + ' GB';
}
