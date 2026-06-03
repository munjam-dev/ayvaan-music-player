const fs = require('fs');

const fixFile = (file) => {
    if (!fs.existsSync(file)) return;
    let content = fs.readFileSync(file, 'utf8');
    
    // Replace naive AsyncImage with optimized one
    const regex = /AsyncImage\(\s*model = ([^,]+?),\s*contentDescription = ([^,]+?),\s*contentScale = ContentScale\.Crop,\s*modifier = Modifier\.fillMaxSize\(\)\s*\)/gs;
    
    content = content.replace(regex, (match, modelExp, desc) => {
        // If it already contains ImageRequest, ignore
        if (modelExp.includes('ImageRequest')) return match;
        
        return `AsyncImage(
            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(${modelExp})
                .size(160)
                .crossfade(true)
                .build(),
            placeholder = androidx.compose.ui.graphics.painter.ColorPainter(androidx.compose.ui.graphics.Color(0xFF222232)),
            contentDescription = ${desc},
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )`;
    });
    
    fs.writeFileSync(file, content, 'utf8');
}

fixFile('app/src/main/java/com/example/ui/screens/HomeScreen.kt');
fixFile('app/src/main/java/com/example/ui/screens/LibraryScreen.kt');
fixFile('app/src/main/java/com/example/ui/screens/PlaylistScreen.kt');
fixFile('app/src/main/java/com/example/ui/screens/SearchScreen.kt');
