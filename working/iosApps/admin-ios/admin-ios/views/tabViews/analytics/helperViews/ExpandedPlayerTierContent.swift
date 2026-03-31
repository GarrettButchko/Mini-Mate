//
//  ExpandedPlayerTierContent.swift
//  MiniMate Manager
//
//  Created by Garrett Butchko on 3/3/26.
//

import SwiftUI
import shared_admin

struct ExpandedPlayerTierContent: View {
    let tier: PlayerTier
    let emails: [String]
    let previewEmails: [String]
    let onCopyToClipboard: () -> Void
    let csvURL: URL?
    
    @State private var showCopiedCheck = false
    
    var body: some View {
        VStack(spacing: 12) {
            // Email list
            if !previewEmails.isEmpty {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Emails (\(min(5, emails.count)) of \(emails.count))")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundStyle(.secondary)
                        .padding(.horizontal, 14)
                        .padding(.top, 10)
                    
                    ScrollView(.vertical, showsIndicators: false) {
                        VStack(alignment: .leading, spacing: 4) {
                            ForEach(previewEmails, id: \.self) { email in
                                Text(email)
                                    .font(.system(.caption2, design: .monospaced))
                                    .foregroundStyle(.secondary)
                                    .lineLimit(1)
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 14)
                    }
                    .frame(maxHeight: 100)
                }
            }
            
            // Action buttons
            VStack(spacing: 8) {
                // Copy to Clipboard button
                Button(action: {
                    onCopyToClipboard()
                    withAnimation(.spring(response: 0.3, dampingFraction: 0.6)) {
                        showCopiedCheck = true
                    }
                    DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                        withAnimation {
                            showCopiedCheck = false
                        }
                    }
                }) {
                    HStack(spacing: 8) {
                        Image(systemName: showCopiedCheck ? "checkmark.circle.fill" : "doc.on.doc")
                            .font(.subheadline)
                            .symbolEffect(.bounce, value: showCopiedCheck)
                        Text(showCopiedCheck ? "Copied!" : "Copy All Emails")
                            .font(.subheadline)
                            .fontWeight(.medium)
                        Spacer()
                    }
                    .frame(maxWidth: .infinity)
                    .padding(10)
                    .background(showCopiedCheck ? Color.green : Color.subThree)
                    .foregroundStyle(.white)
                    .cornerRadius(10)
                }
                
                // Native SwiftUI ShareLink (Reduces UIActivityViewController issues)
                if let url = csvURL {
                    ShareLink(item: url, preview: SharePreview("\(tier.label) Players CSV", icon: Image(systemName: "doc.text"))) {
                        HStack(spacing: 8) {
                            Image(systemName: "square.and.arrow.up")
                                .font(.subheadline)
                            Text("Download CSV")
                                .font(.subheadline)
                                .fontWeight(.medium)
                            Spacer()
                        }
                        .frame(maxWidth: .infinity)
                        .padding(10)
                        .background(.subThree)
                        .foregroundStyle(.white)
                        .cornerRadius(10)
                    }
                } else {
                    // Disabled placeholder if URL is not ready
                    HStack(spacing: 8) {
                        ProgressView()
                            .scaleEffect(0.7)
                            .tint(.white)
                        Text("Preparing CSV...")
                            .font(.subheadline)
                            .fontWeight(.medium)
                        Spacer()
                    }
                    .frame(maxWidth: .infinity)
                    .padding(10)
                    .background(.subThree.opacity(0.5))
                    .foregroundStyle(.white.opacity(0.8))
                    .cornerRadius(10)
                }
                
                Text(tier.description)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .padding(14)
        }
    }
}
