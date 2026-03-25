//
//  NumberPickerView.swift
//  MiniMate
//
//  Created by Garrett Butchko on 4/18/25.
//
import SwiftUI

#if canImport(shared_user)
import shared_user
#elseif canImport(shared_admin)
import shared_admin
#endif

struct NumberPickerView: View {
    @Binding var selectedNumber: Int
    let minNumber: Int?
    let maxNumber: Int

    @State private var localSelection: Int

    init(selectedNumber: Binding<Int>, minNumber: Int?, maxNumber: Int) {
        self._selectedNumber = selectedNumber
        self.minNumber = minNumber
        self.maxNumber = maxNumber
        self._localSelection = State(initialValue: selectedNumber.wrappedValue)
    }

    var body: some View {
        VStack {
            Picker("Select a number", selection: $localSelection) {
                ForEach((minNumber ?? 0)...maxNumber, id: \.self) { number in
                    Text("\(number)").tag(number)
                }
            }
            .pickerStyle(.wheel)
            .frame(height: 95)
            .onChange(of: localSelection) { _, newValue in
                if selectedNumber != newValue {
                    selectedNumber = newValue
                }
            }
            .onChange(of: selectedNumber) { _, newValue in
                if localSelection != newValue {
                    localSelection = newValue
                }
            }
        }
    }
}
