---
name: pixel-sprite-animation
description: Create a pixel-art character or object concept, then animate an approved still image with PixelLab MCP. Use whenever the user asks to make pixel-art game assets, animate a sprite, turn a generated image into an animation, prepare pixel-art assets for Seirei Ruins, or use PixelLab Animate. Preserve a character the user already approved; do not redesign it while preparing animation input.
---

# Pixel Sprite Animation

Create a readable pixel-art still and, when requested, turn that still into a short animation with PixelLab. The game uses small character art, so preserve a clear silhouette, simple color regions, and recognizable features at widget size.

## Workflow

1. **Choose the source image.** If the user points to an image they liked, use that exact image as the animation source. Do not generate a replacement or reinterpret the character. If no source exists, use the `image_gen` skill to create one concise sprite concept and show it for review.
2. **Prepare the image for PixelLab.** Use a PNG with transparency where appropriate, centered with enough padding for motion. PixelLab `animate_image` accepts frames up to 256×256px. Resize a larger source to fit without cropping the silhouette; keep nearest-neighbor/pixel edges when resizing pixel art. Avoid needless palette or style edits.
3. **Check the available PixelLab MCP tools.** Use `mcp__pixellab__animate_image` for a loose sprite image. `animate_character` is for a character already registered in PixelLab and is not the right tool for an arbitrary generated image. If the PixelLab tools are unavailable, say so and leave the source image ready; do not claim that an animation was generated.
4. **Describe motion only.** Give a short action such as “gently bob up and down in place; keep the face and body shape consistent.” Keep the initial pose stable and avoid adding new props, facial features, or large movements unless requested.
5. **Keep the first trial small.** Use 4 frames for a simple idle loop, 8 for walking or a more noticeable loop. `animate_image` requires an even frame count, and its limits depend on image dimensions (`width × height × frame_count ≤ 524288`). Preserve transparency by default; set `no_background` only when the user asks for a solid backdrop.
6. **Be clear about generation cost.** PixelLab generation consumes credits. Before submitting, tell the user the frame count and any returned cost estimate. If they requested a test or animation directly, proceed with a low-cost 4-frame first pass; otherwise ask before starting a billable generation.
7. **Wait for the job.** Generation is asynchronous. Keep the returned job ID, use `mcp__pixellab__wait_for_jobs` while it is running, then fetch the result using `mcp__pixellab__get_image`. The returned sequence includes the unchanged source at index 0 followed by generated frames. Inspect the generated frames for identity drift, transparency, and loop readability.
8. **Deliver usable outputs.** Show the animation or frame previews and provide the PixelLab job ID/link if available. Save files into the project only when the user asks to add them or when the task clearly requests project assets. Do not replace existing art without explicit direction. When saving, preserve frame order and describe the output format and location.

## PixelLab MCP notes

- `animate_image` takes `first_frame_base64` or `first_frame_url` (one only), an `action`, and an even `frame_count`. Prefer a URL if the MCP returns a hosted image URL; otherwise provide a compact PNG as base64 without printing that payload into the conversation.
- The optional last frame can pin a desired ending pose. Use it only when the user supplies or requests a specific ending pose.
- Poll with `wait_for_jobs` rather than repeatedly sleeping. Fetch each needed frame with `get_image(job_id, index)`.
- A 4-frame request returns 5 images including the input. Do not mistake the unchanged first frame for an extra generated pose.
- For a large sprite, reduce dimensions before encoding. The 256×256 maximum and total-pixel limit can reduce the allowed frame count.

## Seirei Ruins art direction

- Prioritize a simple, recognizable silhouette at 28–56dp widget size.
- Keep detail sparse and use distinct flat color clusters. Avoid blur and gradients; inspect generated results because image models may not obey pixel-grid constraints perfectly.
- Use the project's art specifications as guidance, but this skill does not authorize changing those protected design documents or replacing production assets. Record a proposed art-direction change in the relevant GitHub issue before editing design documents.

## Report

Briefly state which source image was used, the motion and frame count, the PixelLab job/result, any deviations noticed, and where outputs were saved. Distinguish a concept preview from a production-ready game asset.
