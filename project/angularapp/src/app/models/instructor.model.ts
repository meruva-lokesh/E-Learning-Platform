/** Shapes of the instructor API (see InstructorController and AdminInstructorController). */
export const INSTRUCTOR_ROLE = 'INSTRUCTOR';
export type ApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export interface InstructorDetails {
  qualification: string;
  experienceYears: number;
  expertise: string;
  bio: string;
  profileLink: string;
}
export interface InstructorRegisterRequest extends InstructorDetails {
  username: string;
  email: string;
  password: string;
  mobileNumber: string;
}
export interface InstructorView extends InstructorDetails {
  userId: number;
  username: string;
  email: string;
  mobileNumber: string;
  status: ApplicationStatus;
  rejectionReason: string | null;
  submittedAt: string | null;
  reviewedAt: string | null;
  reviewedBy: string | null;
}
/** Answer of the public status check (POST /api/instructor/status). No phone number, no bio, a masked email. */
export interface ApplicationStatusView {
  username: string;
  maskedEmail: string;
  status: ApplicationStatus;
  submittedAt: string | null;
  reviewedAt: string | null;
  rejectionReason: string | null;
  /** true = the server asks instructors to verify their mobile number */
  mobileCheckOn: boolean;
  mobileVerified: boolean;
  /** true only for an approved instructor whose mobile (when checked) is verified */
  canLogin: boolean;
}
/** Fixing a pending or rejected application without logging in: the e-mail and password prove who it is.
*/
export interface ResubmitRequest extends InstructorDetails {
  email: string;
  password: string;
}
