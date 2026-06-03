const fs = require('fs');

const fixFile = (file) => {
    if (!fs.existsSync(file)) return;
    let content = fs.readFileSync(file, 'utf8');
    
    // Replace naive AsyncImage with optimized one, capturing model, contentDescription, contentScale and modifier.
    // Making it very permissive to catch all instances.
    const regex = /AsyncImage\(\s*model\s*=\s*(.*?),\s*contentDescription\s*=\s*(.*?),\s*contentScale\s*=\s*(.*?),\s*modifier\s*=\s*(.*?)\s*\)/gs;
    
    content = content.replace(regex, (match, modelExp, descExp, scaleExp, modExp) => {
        // If it already contains ImageRequest or artRequest, ignore
        if (modelExp.includes('ImageRequest') || modelExp.includes('artRequest') || modelExp.includes('builder')) return match;
        
        return `AsyncImage(
            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(${modelExp})
                .size(160)
                .crossfade(true)
                .build(),
            placeholder = androidx.compose.ui.graphics.painter.ColorPainter(androidx.compose.ui.graphics.Color(0xFF222232)),
            contentDescription = ${descExp},
            contentScale = ${scaleExp},
            modifier = ${modExp}
        )`;
    });
    
    fs.writeFileSync(file, content, 'utf8');
}

fixFile('app/src/main/java/com/example/ui/screens/HomeScreen.kt');
fixFile('app/src/main/java/com/example/ui/screens/LibraryScreen.kt');
fixFile('app/src/main/java/com/example/ui/screens/PlaylistScreen.kt');
fixFile('app/src/main/java/com/example/ui/screens/SearchScreen.kt');
