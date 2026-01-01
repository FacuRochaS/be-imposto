package facu.impostor.words;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/words")
public class WordController {
    private final WordRepository wordRepository;
    private final CategoryRepository categoryRepository;

    public WordController(WordRepository wordRepository, CategoryRepository categoryRepository) {
        this.wordRepository = wordRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public List<Word> getAll() {
        return wordRepository.findAll();
    }

    @PostMapping
    public Word create(@RequestBody Word word) {
        if (word.getCategory() != null && word.getCategory().getId() != null) {
            Category cat = categoryRepository.findById(word.getCategory().getId()).orElse(null);
            word.setCategory(cat);
        }
        return wordRepository.save(word);
    }
}

