<?php
/**
 * Gemini API Configuration
 * 
 * IMPORTANT: Replace 'YOUR_GEMINI_API_KEY_HERE' with your actual Gemini API key
 * Get your API key from: https://aistudio.google.com/app/apikey
 */

return [
    'api_key' => 'AIzaSyCIW-7UTVKlfsWyjQE2CUU871s7Hn1YowE', // Your actual API key
    'model' => 'gemini-2.5-flash',
    'timeout' => 30, // seconds
    'max_tokens' => 1000,
    'temperature' => 0.7, // 0.0 to 1.0, lower = more focused, higher = more creative
];
?>
