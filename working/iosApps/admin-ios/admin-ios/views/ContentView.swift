//
//  ContentView.swift
//  MiniMate Manager
//
//  Created by Garrett Butchko on 12/6/25.
//

import SwiftUI
import SwiftData
import shared_admin


struct ContentView: View {
    @EnvironmentObject var authModel: AuthViewModelSwift
    @EnvironmentObject var viewManager: ViewManagerSwift
    @StateObject var viewModel: CourseListViewModelSwift = CourseListViewModelSwift()
    
    let courseRepo = CourseRepository()
    
    @State private var selectedTab = 1
    
    var body: some View {
        ZStack {
            Group {
                switch onEnum(of: viewManager.currentView) {
                case .courseTab(let courseTab):
                    ZStack{
                        CourseTabView(selectedTab: Int(courseTab.tab))
                        ColorPickerView(showColor: $viewModel.showColor, addTarget: $viewModel.addTarget) { color in
                            withAnimation() {
                                guard var course = viewModel.selectedCourse else { return }
                                
                                switch viewModel.addTarget {
                                case .scoreCardColor:
                                    course.scoreCardColorDT = colorToString(color)
                                case .courseColor:
                                    course.courseColorsDT = (course.courseColorsDT ?? []) + [colorToString(color)]
                                case nil:
                                    viewModel.addTarget = nil
                                }
                                
                                viewModel.selectedCourse = course
                                
                                Task{
                                    do {
                                        let _ = try await courseRepo.addOrUpdateCourse(course: course)
                                    } catch {
                                        print("Error updating course color: \(error)")
                                    }
                                }
            
                                viewModel.showColor = false
                            }
                        }
                        .opacity(viewModel.showColor ? 1 : 0)
                        .animation(.spring(duration: 0.25, bounce: 0.4), value: viewModel.showColor)
                        .allowsHitTesting(viewModel.showColor)
                    }
                case .courseList:
                    CourseListView()
                case .welcome:
                    WelcomeView(welcomeText: "Mini Mate Manager", gradientColors: [.managerBlue, .managerGreen])
                case .signIn:
                    SignInView(gradientColors: [.managerBlue, .managerGreen])
                }
            }
        }
        .animation(.easeInOut(duration: 0.1), value: viewManager.currentView)
        .environmentObject(viewModel)
    }
    
    func colorToString(_ color: Color) -> String {
        return String(describing: color)
    }
}

struct CourseTabView: View {
    @Environment(\.modelContext) private var context
    @StateObject private var analyticsVM = AnalyticsViewModelSwift()
    @StateObject private var leaderboardVM = LeaderBoardViewModelSwift()
    @EnvironmentObject var authModel: AuthViewModelSwift
    @EnvironmentObject var VM: CourseListViewModelSwift

    @State var selectedTab: Int
    
    init(selectedTab: Int){
        self.selectedTab = selectedTab
    }
    
    var body: some View {
        TabView(selection: $selectedTab) {
            AnalyticsView()
                .tabItem { Label("Stats", systemImage: "chart.bar.xaxis") }
                .tag(0)
            
            MainView()
                .tabItem { Label("Home", systemImage: "house.fill") }
                .tag(1)
            
            CourseSettingsView()
                .tabItem { Label("Settings", systemImage: "gear") }
                .tag(2)
        }
        .onAppear {
            VM.kotlin.start()
            analyticsVM.kotlin.loadHealthData(course: VM.selectedCourse)
        }
        .onDisappear {
            VM.kotlin.stop()
        }
        .environmentObject(analyticsVM)
        .environmentObject(analyticsVM.growthVM)
        .environmentObject(analyticsVM.operationsVM)
        .environmentObject(analyticsVM.experienceVM)
        .environmentObject(analyticsVM.retentionVM)
        .environmentObject(leaderboardVM)
    }
}
