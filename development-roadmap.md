Development Roadmap
=====================

# Base

Language: Java/Kotlin for backend/logic

# Step 1 Image editing tools

No UI yet, only implementation of interface responsible for aplying features.

```kotlin
interface ImageAdjustmentParams {
    
}

interface ImageAdjustmentProgress {
    val progress: Int
    val result: ImageAdjustmentOperation
}

interface ImageAdjustmentOperation {
    func apply(?);
}

interface ImageAdjustmentFeature {
    func execute(params: ImageAdjustmentParams, image: Image, mask: Mask): ImageAdjustmentProgress
    func preview(params: ImageAdjustmentParams, image: Image, mask: Mask): void

    func createUIComponent(): UIComponent
}

```

`ImageAdjustmentParams` is going to contain parameter value provided by UI that is also part of the adjustment feature, so that it is empty interface implementation of which is completely internal for the feature. Params are also going to be used to save rollback operations.

`ImageAdjustmentProgress`: executing operation is considered async, so progress is returned by execute function. But if operation is instant, can just return progress instantly equal to 100 and containing result.

`ImageAdjustmentOperation` basicaly real operation that is going to be applied. Parameters and contant is not clear now, but should be instant, so if it is having long running backend, it must contain internal state that is applied instantly.

`ImageAdjustmentFeature` main interface for the adjustment feature. Execute is an async (can execute instantly) operation performing actual work on the image to make adjustments. `preview` is a faster version of it when user just moves slider and not yet ready to see final result, but want to see intermediate results. Usually will be used with image preview instad of full size, mask can be more rough, and so on.

*Above structure is the approximate idea and a subject to change*