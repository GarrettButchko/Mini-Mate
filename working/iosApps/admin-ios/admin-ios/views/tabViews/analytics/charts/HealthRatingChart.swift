//
//  HealthRatingChart.swift
//  MiniMate Manager
//
//  Created by Garrett Butchko on 3/4/26.
//

import Charts
import SwiftUI
import shared_admin

struct HealthRatingChart: View {
    @EnvironmentObject var viewModel: CourseListViewModelSwift
    let healthReport: CourseHealthReport
    @State private var animatedScore: Double = 0
    
    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            // Header
            HStack {
                Text("Course Health Rating")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundStyle(.mainOpp)
                Spacer()
                InfoButton(infoText: "...")
            }
            
            VStack(alignment: .center, spacing: 20) {
                
                
                // 2. Center gauge (semicircle style)
                VStack(spacing: 10) {
                    ZStack {
                        
                        ZStack{
                            // Background track
                            GaugeArcShape(
                                lineWidth: 12,
                                startAngle: .degrees(200),
                                endAngle: .degrees(340)
                            )
                            .stroke(
                                Color.subTwoVsColor(makeColor: viewModel.selectedCourse?.scoreCardColor),
                                style: StrokeStyle(lineWidth: 12, lineCap: .round)
                            )

                            
                            // Active arc
                            GaugeArcShape(
                                lineWidth: 12,
                                startAngle: .degrees(200),
                                endAngle: .degrees(200 + (140 * (max(0, min(animatedScore, 100)) / 100)))
                            )
                            .stroke(
                                LinearGradient(colors: gradientColors, startPoint: .leading, endPoint: .trailing),
                                style: StrokeStyle(lineWidth: 12, lineCap: .round)
                            )
                            
                            
                        }
                        .offset(y: 150)
                        
                        VStack(spacing: 2) {
                            RollingNumberText(value: animatedScore, font: .system(size: 40, weight: .semibold, design: .rounded), textColor: healthReport.overallGrade.color)
                                .lineLimit(1)
                            
                            Text(healthReport.overallGrade.rawValue)
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(healthReport.overallGrade.color)
                        }
                    }
                    .frame(width: 160, height: 110)
                }
                .offset(y: 10)
                HStack{
                    VStack{
                        miniStatCard(title: "Growth", score: healthReport.growthHealth.score, grade: healthReport.growthHealth.grade)
                        miniStatCard(title: "Operations", score: healthReport.operationsHealth.score, grade: healthReport.operationsHealth.grade)
                    }
                    VStack{
                        miniStatCard(title: "Retention", score: healthReport.retentionHealth.score, grade: healthReport.retentionHealth.grade)
                        miniStatCard(title: "Experience", score: healthReport.experienceHealth.score, grade: healthReport.experienceHealth.grade)
                    }
                }
            }
            .frame(maxWidth: .infinity)
        }
        .padding()
        .frame(height: 375) // Total container height
        .background {
            RoundedRectangle(cornerRadius: 25)
                .subVsColor(makeColor: viewModel.selectedCourse?.scoreCardColor)
                .cardShadow()
        }
        .onAppear {
            animatedScore = 0
            withAnimation(.easeOut(duration: 1.0)) {
                animatedScore = healthReport.overallScore
            }
        }
        .onChange(of: healthReport.overallScore) { _, newValue in
            withAnimation(.easeOut(duration: 0.8)) {
                animatedScore = newValue
            }
        }
    }
    
    private var gradientColors: [Color] {
        switch animatedScore {
        case 85...:
            return [.green, .green.opacity(0.7)]
        case 70..<85:
            return [.blue, .cyan]
        case 50..<70:
            return [.orange, .yellow]
        default:
            return [.red, .orange]
        }
    }
    
    @ViewBuilder
    private func miniStatCard(title: String, score: Double, grade: HealthGrade, alignmentRL: HorizontalAlignment = .leading, alignmentUD: VerticalAlignment = .top) -> some View {
        VStack{
            
            //if alignmentUD == .bottom{
            //    Spacer()
            //}
            
            HStack{
                if alignmentRL == .trailing{
                    Spacer()
                }
                
                Text(title)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(.mainOpp)
                    .lineLimit(1)
                
                if alignmentRL == .leading {
                    Spacer()
                }
            }
            HStack(spacing: 6) { // Slightly reduced spacing to match smaller text
                
                if alignmentRL == .trailing{
                    Spacer()
                }
                
                Text("\(Int(score))%")
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundStyle(grade.color)
                    .lineLimit(1)
                
                Text(grade.rawValue)
                    .font(.caption)
                    .lineLimit(1)
                    .fontWeight(.semibold)
                    .foregroundStyle(.white)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background {
                        Capsule()
                            .fill(grade.color)
                    }
                
                if alignmentRL == .leading {
                    Spacer()
                }
            }
            
            //if alignmentUD == .top{
            //    Spacer()
            //}
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background {
            RoundedRectangle(cornerRadius: 12)
                .subTwoVsColor(makeColor: viewModel.selectedCourse?.scoreCardColor)
        }
    }
}

private struct GaugeArcShape: Shape {
    let lineWidth: CGFloat
    let startAngle: Angle
    var endAngle: Angle   // must be var for animation interpolation
    
    // Tell SwiftUI which value should animate between old/new states.
    var animatableData: Double {
        get { endAngle.degrees }
        set { endAngle = .degrees(newValue) }
    }
    
    func path(in rect: CGRect) -> Path {
        let radius = min(rect.width, rect.height * 2)
        let center = CGPoint(x: rect.midX, y: rect.minY)
        
        var path = Path()
        path.addArc(
            center: center,
            radius: radius,
            startAngle: startAngle,
            endAngle: endAngle,
            clockwise: false
        )
        return path
    }
}

struct SectionHealthDetailView: View {
    let healthReport: CourseHealthReport
    @State var titleHeight: CGFloat = 0
    
    let bottomInset: CGFloat = 40
    
    var body: some View {
        ZStack{
            ScrollView{
                scrollViewContent
                Spacer()
                    .frame(height: bottomInset)
            }
            .contentMargins(.top, titleHeight)
            .contentMargins(16)
            .mask {
                VStack(spacing: 0) {
                    LinearGradient(
                        colors: [.clear, .black],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .frame(height: titleHeight + 16)
                    
                    Rectangle()
                        .fill(.black)
                    
                    LinearGradient(
                        colors: [.black, .clear],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .frame(height: bottomInset)
                }
            }
            
            VStack{
                HStack {
                    Text("Health Breakdown")
                        .font(.title2)
                        .fontWeight(.bold)
                    Spacer()
                }
                .padding(.top, 32)
                .padding(.horizontal)
                .padding(.bottom)
                .background {
                    GeometryReader { proxy in
                        Color.clear
                            .task(id: proxy.size) {
                                titleHeight = proxy.size.height
                            }
                    }
                }
                Spacer()
            }
        }
        .ignoresSafeArea()
    }
    
    private var scrollViewContent: some View {
        VStack(alignment: .leading, spacing: 16) {
            VStack(spacing: 12) {
                HealthSectionCard(rating: healthReport.growthHealth)
                HealthSectionCard(rating: healthReport.operationsHealth)
                HealthSectionCard(rating: healthReport.experienceHealth)
                HealthSectionCard(rating: healthReport.retentionHealth)
            }
            
            if !healthReport.topInsights.isEmpty {
                HStack {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Key Insights")
                            .font(.headline)
                        
                        ForEach(Array(healthReport.topInsights.prefix(5).enumerated()), id: \.offset) { _, group in
                            InsightRowView(insight: group.first!, section: group.second! as AnalyticsSection)
                        }
                    }
                    Spacer()
                }
                .padding()
                .background {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(.subTwo)
                }
            }
        }
    }
}

// MARK: - Extracted Subviews

private struct HealthSectionCard: View {
    let rating: SectionHealthRating
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            SectionCardHeaderView(rating: rating)
            SectionCardProgressBarView(score: rating.score, gradeColor: rating.grade.color)
            
            if !rating.insights.isEmpty {
                SectionCardInsightsListView(insights: rating.insights)
            }
        }
        .padding()
        .background {
            RoundedRectangle(cornerRadius: 12)
                .fill(.subTwo)
                .overlay {
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(rating.section.color.opacity(0.3), lineWidth: 1)
                }
        }
    }
}

private struct InsightRowView: View {
    let insight: Insight
    let section: AnalyticsSection
    
    var body: some View {
        HStack(alignment: .center, spacing: 10) {
            Image(systemName: insight.imageName)
                .font(.footnote)
                .foregroundStyle(insight.color)
                .frame(width: 20, alignment: .center)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(section.rawValue.uppercased())
                    .font(.caption2.bold())
                    .foregroundStyle(section.color)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background {
                        RoundedRectangle(cornerRadius: 4)
                            .stroke(section.color, lineWidth: 1)
                    }
                
                Text(insight.description_)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(.vertical, 4)
    }
}

private struct SectionCardHeaderView: View {
    let rating: SectionHealthRating
    
    var body: some View {
        HStack {
            Text(rating.section.rawValue)
                .font(.headline)
            
            Spacer()
            
            HStack(spacing: 8) {
                Text("\(Int(rating.score))%")
                    .font(.title3)
                    .fontWeight(.bold)
                    .foregroundStyle(rating.grade.color)
                
                Text(rating.grade.rawValue)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundStyle(.white)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background {
                        Capsule()
                            .fill(rating.grade.color)
                    }
            }
        }
    }
}

private struct SectionCardProgressBarView: View {
    let score: Double
    let gradeColor: Color
    
    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .leading) {
                RoundedRectangle(cornerRadius: 4)
                    .fill(.gray.opacity(0.2))
                    .frame(height: 8)
                
                RoundedRectangle(cornerRadius: 4)
                    .fill(gradeColor)
                    .frame(width: geometry.size.width * (score / 100), height: 8)
            }
        }
        .frame(height: 8)
    }
}

private struct SectionCardInsightsListView: View {
    let insights: [Insight]
    
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            ForEach(Array(insights.prefix(2).enumerated()), id: \.offset) { _, insight in
                HStack(alignment: .center, spacing: 8) {
                    Image(systemName: insight.imageName)
                        .font(.caption)
                        .foregroundStyle(insight.color)
                        .frame(width: 16, height: 16)
                    
                    Text(insight.description_)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(3)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(.vertical, 6)
            }
        }
        .padding(.top, 4)
    }
}
