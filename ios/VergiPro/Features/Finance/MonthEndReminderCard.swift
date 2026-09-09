import SwiftUI

struct MonthEndReminderCard: View {
    let reminderStatus: MonthEndReminderStatusDTO?
    var onTriggerReminder: (() async -> TriggerReminderResponse?)? = nil
    var onNavigateToCapture: (() -> Void)? = nil

    @State private var isSending = false
    @State private var sendFeedback: String? = nil
    @State private var sendSucceeded: Bool? = nil
    @State private var isCopied = false

    private var daysLeft: Int? {
        reminderStatus?.daysRemaining
    }

    private var isUrgent: Bool {
        reminderStatus?.isUrgent ?? daysLeft.map { $0 <= 5 } ?? false
    }

    var body: some View {
        VStack(alignment: .leading, spacing: VPSpace.md) {
            // Header
            HStack {
                HStack(spacing: 8) {
                    Image(systemName: "calendar.badge.clock")
                        .font(.headline)
                        .foregroundStyle(isUrgent ? VPColor.danger : VPColor.warning)
                    Text("reminder_radar_title")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(VPColor.secondaryText)
                        .tracking(0.8)
                }

                Spacer()

                if let targetDay = reminderStatus?.targetDay, targetDay > 0 {
                    Text(String(format: String(localized: "finance.reminder.target_day"), targetDay))
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(VPColor.secondaryText)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(VPColor.canvas, in: Capsule())
                }
            }

            // Countdown & Info
            HStack(alignment: .center, spacing: VPSpace.md) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(daysLeft.map { String(format: String(localized: "finance.days_remaining"), $0) } ?? String(localized: "reminder_schedule_unavailable"))
                        .font(.title2.bold().monospacedDigit())
                        .foregroundStyle(isUrgent ? VPColor.danger : VPColor.textPrimary)
                    Text("reminder_description")
                        .font(.caption)
                        .foregroundStyle(VPColor.secondaryText)
                }

                Spacer()

                if let onNavigateToCapture {
                    Button(action: onNavigateToCapture) {
                        HStack(spacing: 4) {
                            Image(systemName: "camera.fill")
                            Text("reminder_scan_receipt")
                        }
                        .font(.caption.bold())
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(VPColor.brand, in: RoundedRectangle(cornerRadius: 10))
                        .foregroundStyle(VPColor.brandInverse)
                    }
                    .buttonStyle(.plain)
                }
            }

            // Telegram & Team status
            if let reminderStatus {
                HStack(spacing: VPSpace.sm) {
                    HStack(spacing: 6) {
                        Image(systemName: "paperplane.fill")
                            .font(.caption)
                            .foregroundStyle(VPColor.accentBlue)
                        Text(reminderStatus.botUsername.isEmpty ? String(localized: "reminder_bot_unavailable") : "@\(reminderStatus.botUsername)")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(VPColor.textPrimary)
                    }

                    Spacer()

                    Button {
                        guard !reminderStatus.botInviteCode.isEmpty else { return }
                        UIPasteboard.general.string = reminderStatus.botInviteCode
                        withAnimation { isCopied = true }
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                            withAnimation { isCopied = false }
                        }
                    } label: {
                        HStack(spacing: 4) {
                            Image(systemName: isCopied ? "checkmark" : "doc.on.doc")
                            Text(isCopied ? String(localized: "finance.reminder.copied") : reminderStatus.botInviteCode.isEmpty ? String(localized: "reminder_code_unavailable") : String(format: String(localized: "reminder_code"), reminderStatus.botInviteCode))
                        }
                        .font(.caption2.bold().monospaced())
                        .foregroundStyle(isCopied ? VPColor.success : VPColor.accentBlue)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(VPColor.accentBlue.opacity(0.10), in: RoundedRectangle(cornerRadius: 6))
                    }
                    .buttonStyle(.plain)
                    .disabled(reminderStatus.botInviteCode.isEmpty)
                }
                .padding(10)
                .background(VPColor.canvas, in: RoundedRectangle(cornerRadius: VPRadius.medium))
            }

            // Action: Trigger Reminder to All Team
            if let onTriggerReminder {
                Button {
                    Task {
                        isSending = true
                        sendSucceeded = nil
                        let resp = await onTriggerReminder()
                        isSending = false
                        if let resp, resp.success {
                            sendSucceeded = true
                            sendFeedback = String(format: String(localized: "reminder_sent_count"), resp.totalRecipients)
                        } else {
                            sendSucceeded = false
                            sendFeedback = String(localized: "reminder_failed")
                        }
                    }
                } label: {
                    HStack {
                        if isSending {
                            ProgressView().tint(VPColor.brandInverse)
                        } else {
                            Image(systemName: "bell.badge.fill")
                            Text("reminder_send_team")
                                .font(.subheadline.weight(.semibold))
                        }
                    }
                    .frame(maxWidth: .infinity, minHeight: 44)
                    .background(VPColor.brand)
                    .foregroundStyle(VPColor.brandInverse)
                    .clipShape(RoundedRectangle(cornerRadius: VPRadius.medium))
                }
                .buttonStyle(.plain)
                .disabled(isSending)

                if let sendFeedback {
                    HStack(spacing: 6) {
                        Image(systemName: sendSucceeded == true ? "checkmark.circle.fill" : "exclamationmark.triangle.fill")
                            .foregroundStyle(sendSucceeded == true ? VPColor.success : VPColor.danger)
                        Text(sendFeedback)
                            .font(.caption2.bold())
                            .foregroundStyle(sendSucceeded == true ? VPColor.success : VPColor.danger)
                    }
                }
            }
        }
        .vpCard()
    }
}
