//
//  TrendChart.swift
//  MiniMate
//
//  Created by Garrett Butchko on 2/26/26.
//

import Charts
import SwiftUI
import shared_admin

struct PlayerTrendChart: View {
    @EnvironmentObject var VM: GrowthViewModelSwift
    
    // Mimicking the dates and curve from your screenshot
    @State var data: [PlayerActivity] = []
    
    var lineColor: Color = .purple
    
    init(VM: GrowthViewModelSwift){
        lineColor = VM.growthChartTopic.color
    }

    private var isDataEmpty: Bool {
        data.isEmpty || data.allSatisfy({ $0.count == 0 })
    }

    var body: some View {
        Group {
            if isDataEmpty {
                emptyStateView
            } else {
                chartContent
            }
        }
        .task {
            do {
                 data = try await VM.getDataForGrowthTrend()
            } catch {
                print("Error fetching Growth trend data: \(error)")
            }
        }
    }
    
    @ViewBuilder
    private var emptyStateView: some View {
        VStack(spacing: 10) {
            Image(systemName: "chart.line.uptrend.xyaxis")
                .font(.system(size: 28, weight: .semibold))
                .foregroundStyle(.secondary)
            Text("Not enough data yet")
                .font(.headline)
                .fontWeight(.semibold)
            Text("Player trend data will appear here once players start visiting your course.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 12)
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(RoundedRectangle(cornerRadius: 16).fill(.subTwo))
    }
    
    @ViewBuilder
    private var chartContent: some View {
        Chart {
            ForEach(data) { item in
                let date = item.date.toNSDate()
                
                LineMark(
                    x: .value("Day", date, unit: .day),
                    y: .value("Players", item.count)
                )
                .foregroundStyle(lineColor)
                .lineStyle(StrokeStyle(lineWidth: 2, lineCap: .round))
                
                AreaMark(
                    x: .value("Day", date, unit: .day),
                    y: .value("Players", item.count)
                )
                .foregroundStyle(
                    .linearGradient(
                        colors: [lineColor.opacity(0.4), .clear],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
            }
        }
        .chartPlotStyle { plot in
            plot
                .frame(maxHeight: .infinity, alignment: .top)
                .padding(.bottom, 8)
        }
        .chartYScale(domain: .automatic(includesZero: true))
        .chartYAxis {
            AxisMarks(position: .leading, values: .automatic(desiredCount: 5)) { value in
                AxisGridLine()
                AxisValueLabel()
            }
        }
        .chartXAxis {
            AxisMarks(preset: .aligned, values: getStrideValues(from: data)) { value in
                AxisGridLine()
                    .foregroundStyle(.secondary.opacity(0.3))
                    
                AxisValueLabel {
                    if let date = value.as(Date.self) {
                        Text(date, format: .dateTime.month().day())
                    }
                }
            }
        }
        .frame(height: 220)
        .padding()
        .padding(.top)
        .background {
            RoundedRectangle(cornerRadius: 17)
                .fill(.subTwo)
        }
    }
    
    func getStrideValues(from data: [PlayerActivity]) -> [Date] {
        let count = data.count
        
        if count <= 5 {
            return data.map { $0.date.toNSDate() }
        }
        
        guard let first = data.first?.date.toNSDate(), let last = data.last?.date.toNSDate() else { return [] }
        
        let diff = last.timeIntervalSince(first)
        let step = diff / 5
        
        return [
            first.addingTimeInterval(step),     
            first.addingTimeInterval(step * 2), 
            first.addingTimeInterval(step * 3), 
            first.addingTimeInterval(step * 4)  
        ]
    }
}

struct GameDurationTrendChart: View {
    @EnvironmentObject var VM: OperationsViewModelSwift
    
    @State var data: [GameDurationActivity] = []
    var lineColor: Color = .cyan
    
    init(VM: OperationsViewModelSwift){
        self.lineColor = .cyan
    }
    
    private var isDataEmpty: Bool {
        data.isEmpty || data.allSatisfy({ $0.avgMinutes == 0 })
    }
    
    var body: some View {
        Group {
            if isDataEmpty {
                emptyStateView
            } else {
                mainContent
            }
        }
        .task {
            do {
                 data = try await VM.getDataForDurationTrend()
            } catch {
                print("Error fetching duration trend data: \(error)")
            }
        }
        .id(VM.rangeDailyDocs.count) // Force redraw when data changes
    }
    
    @ViewBuilder
    private var emptyStateView: some View {
        VStack(spacing: 10) {
            Image(systemName: "timer")
                .font(.system(size: 28, weight: .semibold))
                .foregroundStyle(.secondary)
            Text("Not enough data yet")
                .font(.headline)
                .fontWeight(.semibold)
            Text("Game duration trends will appear here once players start completing games.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 12)
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(RoundedRectangle(cornerRadius: 17).fill(.subTwo))
    }
    
    @ViewBuilder
    private var mainContent: some View {
        VStack{
            HStack(alignment: .center) {
                Text("Game Duration Over Time")
                    .foregroundStyle(.mainOpp)
                    .font(.system(size: 14, weight: .semibold))
                Spacer()
                InfoButton(infoText: "Shows the duration of the games over time.")
            }
            .padding(.bottom, 16)
            
            chartView
        }
        .frame(height: 200)
        .padding()
        .background(RoundedRectangle(cornerRadius: 17).fill(.subTwo))
    }
    
    @ViewBuilder
    private var chartView: some View {
        Chart {
            ForEach(data) { item in
                let date = item.date.toNSDate()
                
                LineMark(
                    x: .value("Day", date, unit: .day),
                    y: .value("Minutes", item.avgMinutes)
                )
                .foregroundStyle(lineColor)
                .lineStyle(StrokeStyle(lineWidth: 2.5, lineCap: .round))
                
                AreaMark(
                    x: .value("Day", date, unit: .day),
                    y: .value("Minutes", item.avgMinutes)
                )
                .foregroundStyle(
                    .linearGradient(
                        colors: [lineColor.opacity(0.3), .clear],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
            }
        }
        .chartYScale(domain: .automatic(includesZero: true))
        .chartYAxis {
            AxisMarks(position: .leading, values: .automatic(desiredCount: 4)) { value in
                AxisGridLine()
                    .foregroundStyle(.secondary.opacity(0.2))
                AxisValueLabel {
                    if let minutes = value.as(Double.self) {
                        Text("\(Int(minutes))m")
                            .font(.caption2)
                    }
                }
            }
        }
        .chartXAxis {
            AxisMarks(preset: .aligned, values: getStrideValues(from: data)) { value in
                AxisGridLine()
                    .foregroundStyle(.secondary.opacity(0.3))
                AxisValueLabel {
                    if let date = value.as(Date.self) {
                        Text(date, format: .dateTime.month().day())
                            .font(.caption2)
                    }
                }
            }
        }
    }
    
    func getStrideValues(from data: [GameDurationActivity]) -> [Date] {
        let count = data.count
        
        if count <= 5 {
            return data.map { $0.date.toNSDate() }
        }
        
        guard let first = data.first?.date.toNSDate(), let last = data.last?.date.toNSDate() else { return [] }
        
        let diff = last.timeIntervalSince(first)
        let step = diff / 5
        
        return [
            first.addingTimeInterval(step),
            first.addingTimeInterval(step * 2),
            first.addingTimeInterval(step * 3),
            first.addingTimeInterval(step * 4)
        ]
    }
}
