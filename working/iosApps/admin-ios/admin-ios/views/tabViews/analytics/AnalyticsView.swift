//
//  AnalyticsView.swift
//  MiniMate Manager
//
//  Created by Garrett Butchko on 12/18/25.
//

import SwiftUI
import Combine
import Foundation
import shared_admin

struct AnalyticsView: View {
    
    @EnvironmentObject var courseVM: CourseListViewModelSwift
    @EnvironmentObject var VM: AnalyticsViewModelSwift
    
    let anaRepo = AnalyticsRepository()
    
    @State private var topBarHeight: CGFloat = 140
    @State private var canManualRefresh = true
    @State private var refreshRotation: Double = 0
    private let refreshCooldown: TimeInterval = 3
    
    @State var isRotating: Bool = false
    
    var body: some View {
        if let course = courseVM.selectedCourse {
            if course.tier <= 2 {
                TierLockView()
            } else {
                VStack {
                    headerSection
                    
                    ZStack (alignment: .top){
                        if VM.pickedSection == "Day Range" {
                            dayRangecontent
                            topBar
                        } else {
                            RetentionView()
                                .environmentObject(VM.retentionVM)
                        }
                    }
                }
                .environmentObject(VM)
                .background(.bg)
                .onChange(of: VM.range) { old, new in
                    withAnimation {
                        VM.onChange(old: old, new: new, course: courseVM.selectedCourse)
                    }
                }
            }
        }
    }
    
    private var headerSection: some View {
        VStack (spacing: 8){
            HStack{
                VStack(alignment: .leading){
                    Text("Analytics")
                        .font(.title)
                        .fontWeight(.bold)
                }
                
                Spacer()
                
                refreshButton

                
                #if DEBUG
                    debugMenu
                #endif
            }
            
            Picker("Section", selection: $VM.pickedSection) {
                ForEach(VM.pickerSections, id: \.self) {
                    Text($0)
                }
            }
            .pickerStyle(.segmented)
        }
        .padding([.horizontal, .top])
    }
    
    private var refreshButton: some View {
        Button(action: {
            withAnimation {
                isRotating = true
                triggerAnalyticsRefresh(isAnalytics: VM.pickedSection == "Day Range")
                DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                    isRotating = false
                }
            }
        }) {
            Image(systemName: "arrow.trianglehead.2.clockwise")
                .rotationEffect(.degrees(isRotating ? 360 : 0))
                .font(.title2)
                .foregroundColor(.blue)
                .frame(width: 30, height: 30)
        }
        .disabled(!canManualRefresh)
        .opacity(canManualRefresh ? 1 : 0.45)
    }
    
    #if DEBUG
    private var debugMenu: some View {
        Menu {
            Button {
                Task {
                    do {
                        let _ = try await anaRepo.uploadDebugDailyDocs(courseID: courseVM.selectedCourse!.id, days: 90)
                        VM.refreshAnalytics(course: courseVM.selectedCourse)
                    } catch {
                        print("\(error)")
                    }
                }
            } label: {
                Label("Upload Daily Docs", systemImage: "calendar")
            }
            
            Button {
                Task {
                    do {
                        let _ = try await anaRepo.uploadDebugEmails(courseID: courseVM.selectedCourse!.id, count: 100)
                        VM.refreshAnalytics(course: courseVM.selectedCourse)
                    } catch {
                        print("\(error)")
                    }
                }
            } label: {
                Label("Upload 100 Test Emails", systemImage: "envelope.badge.fill")
            }
        } label: {
            Image(systemName: "hammer.fill")
                .foregroundStyle(.secondary)
        }
    }
    #endif
    
    var dayRangecontent: some View {
        ScrollViewReader { proxy in
            ScrollView(.vertical) {
                VStack(spacing: 0){
                    Color.clear
                        .frame(height: 0)
                        .id("top")
                    if VM.loadingDocs {
                        LoadingSkeleton()
                    } else {
                        tabContent
                    }
                }
            }
            .contentMargins([.horizontal, .bottom], 16)
            .contentMargins(.top, topBarHeight)
            .onChange(of: VM.selectedSection) { _, _ in
                proxy.scrollTo("top", anchor: .top)
            }
            .onChange(of: VM.range) { _, _ in
                withAnimation {
                    proxy.scrollTo("top", anchor: .top)
                }
            }
        }
    }
    
    @ViewBuilder
    private var tabContent: some View {
        switch VM.selectedSection {
        case .growth:
            GrowthTab()
                .environmentObject(VM.growthVM)
                .transition(.opacity)
                
        case .operations:
            OperationsTab()
                .environmentObject(VM.operationsVM)
                .frame(maxWidth: .infinity)
                .transition(.opacity)
                
        case .experience:
            ExpierenceTab()
                .environmentObject(VM.experienceVM)
                .frame(maxWidth: .infinity)
                .transition(.opacity)
                
        default:
            EmptyView()
        }
        
    }

    var topBar: some View {
        VStack (spacing: 16){
            sectionSelector
            AnalyticsRangeBar()
                .padding([.horizontal, .bottom], 16)
        }
        .background {
            GeometryReader { proxy in
                RoundedRectangle(cornerRadius: 25)
                    .ifAvailableGlassEffect()
                    .cardShadow()
                    .task(id: proxy.size) {
                        await MainActor.run {
                            withAnimation(.easeInOut(duration: 0.3)) {
                                topBarHeight = proxy.size.height + 24
                            }
                        }
                    }
            }
        }
        .padding(.horizontal)
        .padding(.top, 8)
        .background(
            LinearGradient(
                gradient: Gradient(colors: [Color.bg, Color.clear]),
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea(edges: .top)
        )
    }
    
    private var sectionSelector: some View {
        HStack {
            ForEach(AnalyticsSection.allCases) { section in
                if let obj = VM.analyticsObjects[section.rawValue] {
                    SectionButton(section: section, obj: obj, selection: $VM.selectedSection)
                }
            }
        }
        .padding([.horizontal, .top], 16)
    }
    
    private func triggerAnalyticsRefresh(isAnalytics: Bool = true) {
        guard canManualRefresh else { return }
        canManualRefresh = false

        if isAnalytics {
            VM.refreshAnalytics(course: courseVM.selectedCourse)
        } else {
            Task {
                do {
                    try await VM.onAppearRetention(course: courseVM.selectedCourse)
                } catch {
                    print("\(error)")
                }
            }
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + refreshCooldown) {
            canManualRefresh = true
        }
    }
}

// MARK: - Helper Views

struct TierLockView: View {
    var body: some View {
        VStack(spacing: 0) {
            Spacer()
            
            VStack(spacing: 28) {
                ZStack {
                    Circle()
                        .fill(Color.blue.opacity(0.1))
                        .frame(width: 120, height: 120)
                    
                    Image(systemName: "chart.pie.fill")
                        .font(.system(size: 60))
                        .foregroundStyle(
                            LinearGradient(
                                colors: [.blue, .purple],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                    
                    Image(systemName: "lock.fill")
                        .font(.system(size: 24))
                        .padding(8)
                        .background(Circle().fill(.bg))
                        .offset(x: 35, y: 35)
                        .foregroundStyle(.secondary)
                }
                
                VStack(spacing: 12) {
                    Text("Advanced Analytics")
                        .font(.system(size: 28, weight: .bold, design: .rounded))
                        .foregroundStyle(.mainOpp)
                    
                    Text("Unlock deep insights into player retention, peak hours, and course performance with MiniMate Pro.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 20)
                }
                
                VStack(alignment: .leading, spacing: 18) {
                    FeatureItem(icon: "calendar.badge.clock",
                                title: "Day Range Analysis",
                                description: "Track performance over custom time periods.")
                    
                    FeatureItem(icon: "person.2.wave.2.fill",
                                title: "Retention Tracking",
                                description: "See how many players return to your course.")
                    
                    FeatureItem(icon: "arrow.down.doc.fill",
                                title: "Data Export",
                                description: "Download CSV reports for your business records.")
                }
                .padding(.vertical, 8)
                
                VStack(spacing: 16) {
                    Button {
                        // Action to open subscription/billing page
                    } label: {
                        Text("Upgrade to Tier 3")
                            .font(.headline)
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 54)
                            .background(Color.blue)
                            .cornerRadius(16)
                            .shadow(color: .blue.opacity(0.3), radius: 10, y: 5)
                    }
                    
                    Text("Compare Tiers in Settings")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .padding(.horizontal, 32)
            
            Spacer()
        }
    }
}

struct FeatureItem: View {
    let icon: String
    let title: String
    let description: String
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 18))
                .foregroundStyle(.blue)
                .frame(width: 32, height: 32)
                .background(Circle().fill(.blue.opacity(0.15)))
            
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundStyle(.mainOpp)
                
                Text(description)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }
}

struct SectionButton: View {
    let section: AnalyticsSection
    let obj: AnalyticsObject
    @Binding var selection: AnalyticsSection
    
    // Explicitly resolving the color into a non-optional property to assist the compiler
    private var themeColor: Color {
        obj.color.toColor() ?? Color.mainOpp
    }
    
    var body: some View {
        Button {
            withAnimation(.snappy) {
                selection = section
            }
        } label: {
            HStack(spacing: 6) {
                Image(systemName: obj.icon)
                
                if selection == section {
                    Text(section.rawValue)
                        .fontWeight(.bold)
                        .frame(maxWidth: .infinity)
                }
            }
        }
        .id(section)
        .foregroundStyle(themeColor)
        .padding(.vertical, 8)
        .padding(.horizontal, 16)
        .background(
            selection == section ? themeColor.opacity(0.2) : Color.clear
        )
        .overlay(
            RoundedRectangle(cornerRadius: 25)
                .stroke(themeColor.opacity(0.3), lineWidth: 4)
                .opacity(selection != section ? 0.5 : 0)
        )
        .clipShape(RoundedRectangle(cornerRadius: 25))
    }
}

struct LoadingSkeleton: View {
    var body: some View {
        VStack(spacing: 16) {
            VStack {
                RoundedRectangle(cornerRadius: 17).fill(.subTwo).frame(height: 100)
                HStack {
                    RoundedRectangle(cornerRadius: 17).fill(.subTwo).frame(height: 100)
                    RoundedRectangle(cornerRadius: 17).fill(.subTwo).frame(height: 100)
                }
            }
            .skeleton(active: true)
            .clipShape(RoundedRectangle(cornerRadius: 17))
            
            RoundedRectangle(cornerRadius: 17)
                .fill(.subTwo)
                .frame(height: 220)
                .skeleton(active: true)
                .clipShape(RoundedRectangle(cornerRadius: 17))
        }
        .padding()
        .background {
            RoundedRectangle(cornerRadius: 25)
                .fill(.sub)
                .cardShadow()
        }
        .transition(.opacity)
    }
}

struct HoleDifficultyCharts: View {
    @EnvironmentObject var VM: ExperienceViewModelSwift
    @State var difficultyData: [HoleDifficultyData] = []
    
    var body: some View {
        VStack(spacing: 16) {
            if !difficultyData.isEmpty {
                HoleDifficultyChart(difficultyData: $difficultyData)
                HoleHardnessPreviewList(difficultyData: $difficultyData)
            } else {
                emptyDataView
            }
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(RoundedRectangle(cornerRadius: 17).fill(.subTwo))
        .task {
            await loadData()
        }
    }
    
    private var emptyDataView: some View {
        VStack(spacing: 10) {
            Image(systemName: "chart.bar.xaxis")
                .font(.system(size: 28, weight: .semibold))
                .foregroundStyle(.secondary)
            
            Text("Not enough data yet")
                .font(.headline)
                .fontWeight(.semibold)
            
            Text("Wait for a few more holes to be submitted on this course to unlock hole difficulty stats.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 12)
        }
    }
    
    private func loadData() async {
        do {
            difficultyData = try await VM.getHoleDifficultyData()
        } catch {
            print("Error fetching hole difficulty data: \(error)")
        }
    }
}

struct HoleDifficultyParCharts: View {
    @EnvironmentObject var VM: ExperienceViewModelSwift
    @State var difficultyData: [HoleHeatmapData] = []
    let course: Course
    
    var body: some View {
        VStack(spacing: 16) {
            if !difficultyData.isEmpty {
                HoleDifficultyParChart(difficultyData: $difficultyData)
                    .frame(height: 100)
                
                HolePreviewList(allHoles: $difficultyData)
            } else {
                emptyDataView
            }
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(RoundedRectangle(cornerRadius: 17).fill(.subTwo))
        .task {
            await loadData()
        }
    }
    
    private var emptyDataView: some View {
        VStack(spacing: 10) {
            Image(systemName: "chart.bar.xaxis")
                .font(.system(size: 28, weight: .semibold))
                .foregroundStyle(.secondary)
            
            Text("Not enough data yet")
                .font(.headline)
                .fontWeight(.semibold)
            
            Text("Wait for a few more holes to be submitted on this course to unlock par performance stats.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 12)
        }
    }
    
    private func loadData() async {
        do {
            difficultyData = try await VM.getHoleHeatmapForParData(course: course)
        } catch {
            print("Error fetching hole heatmap data: \(error)")
        }
    }
}

struct InfoButton: View {
    @State var showInfo: Bool = false
    let infoText: String
    
    var body: some View {
        Button {
            showInfo = true
        } label: {
            Image(systemName: "info.circle")
                .resizable()
                .scaledToFit()
                .frame(width: 20, height: 20)
                .foregroundStyle(.blue)
        }
        .alert("Info", isPresented: $showInfo) {
            Button("OK") {}
        } message: {
            Text(infoText)
        }
    }
}
