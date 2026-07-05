# Decisions

| ADR | Status | Summary | Read when |
|---|---|---|---|
| [ADR-0001](ADR-0001-computed-account-balance-from-ledger.md) | Active | Compute account balance from `initial_balance + SUM(signed_amount)` over confirmed ledger rows; no mutable balance total. | Changing balance, STS, net-worth, projection, transaction sign, or account/budget SQL. |
| [ADR-0002](ADR-0002-reactive-cross-repository-invalidation.md) | Active | Transaction writes emit `transactionChangedTrigger`; dependent repos use `flatMapLatest` to force fresh SQLDelight queries. | Adding transaction mutators, repository flows, or transaction-dependent queries. |
| [ADR-0003](ADR-0003-single-compose-ui-android-ios.md) | Active | Android and iOS share one Compose UI; iOS SwiftUI is only a thin host for `MainViewController()`. | Adding screens, navigation, feature UI, or iOS app entrypoints. |
