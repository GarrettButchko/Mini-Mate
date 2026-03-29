import SwiftUI
import shared_admin

struct ColorPickerView: View {
    @EnvironmentObject var viewModel: CourseListViewModelSwift
    
    // Moved to a constant or static property for better performance
    let colors: [Color] = [.red, .orange, .yellow, .green, .blue, .indigo, .purple, .pink, .brown]
    
    @Binding var showColor: Bool
    @Binding var addTarget: ColorAddTarget?
    let function: (_ color: Color) -> Void
    
    var body: some View {
        ZStack {
            // Background dimming - simplified
            Color.black.opacity(0.25)
                .ignoresSafeArea()
                .onTapGesture { dismiss() } // Allow tapping outside to dismiss
            
            // Popup card
            VStack(spacing: 24) {
                headerSection
                
                colorGrid
                
                cancelButton
            }
            .padding(24)
            .background {
                RoundedRectangle(cornerRadius: 38, style: .continuous)
                    .ifAGENoDefaultColor()
                    .cardShadow()
            }
            .padding(.horizontal, 30)
            .transition(.scale(scale: 0.9).combined(with: .opacity))
        }
    }
    
    // MARK: - Subviews
    private var headerSection: some View {
        HStack {
            Spacer()
            Text("Pick a Color")
                .font(.system(.title3, weight: .regular))
            Spacer()
        }
    }
    
    private var colorGrid: some View {
        LazyVGrid(columns: Array(repeating: .init(.flexible()), count: 4), spacing: 16) {
            ForEach(colors, id: \.self) { color in
                let isDisabled = isColorDisabled(color)
                
                Button {
                    function(color)
                    dismiss()
                } label: {
                    Circle()
                        .fill(.mainOpp.opacity(0.15))
                        .frame(width: 44, height: 44) // Increased touch target
                        .overlay {
                            Circle()
                                .fill(color)
                                .frame(width: 32, height: 32)
                                .opacity(isDisabled ? 0.2 : 1.0)
                                .overlay {
                                    if isDisabled {
                                        Image(systemName: "lock.fill")
                                            .font(.system(size: 10))
                                            .foregroundStyle(.secondary)
                                    }
                                }
                        }
                }
                .disabled(isDisabled)
                .buttonStyle(PlainButtonStyle()) // Removes default button flash
            }
        }
    }
    
    private var cancelButton: some View {
        Button(action: dismiss) {
            Text("Cancel")
                .font(.title3)
                .foregroundStyle(.mainOpp)
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                
                .background{
                    Capsule()
                        .fill(.mainOpp.opacity(0.15))
                }
        }
        .buttonStyle(.plain)
    }
    
    // MARK: - Logic
    
    private func isColorDisabled(_ color: Color) -> Bool {
        guard addTarget != .scoreCardColor else { return false }
        return viewModel.selectedCourse?.courseColors?.contains(color) ?? false
    }
    
    private func dismiss() {
        withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
            showColor = false
        }
    }
}
