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

    var body: some View {
        VStack {
            Picker("Select a number", selection: $selectedNumber) {
                ForEach((minNumber ?? 0)...maxNumber, id: \.self) { number in
                    Text("\(number)").tag(number)
                }
            }
            .pickerStyle(.wheel)
            .frame(height: 95)
        }
    }
}

