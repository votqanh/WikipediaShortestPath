package cpen221.mp3.wikimediator;

import cpen221.mp3.fsftbuffer.Bufferable;

public class WikiPage implements Bufferable {
    private final String title;
    private final String text;

    /* Representation Invariant */
    // title and text are not null
    // title exists as the actual title of a Wikipedia page
    // text corresponds to the actual text of the Wikipedia page with the title of the page being title

    /* Abstraction Function */
    // An instance of WikiPage represents a page on Wikipedia.

    /**
     * Private method to check that the representation invariant holds, not present in any of the final
     * code for performance reasons.
     */
    private void checkRep() {
        WikiMediator mediator = new WikiMediator(100, 10);
        assert !title.equals(null);
        assert !text.equals(null);
        assert mediator.getPage(title).equals(text);
    }

    /**
     * Creates a WikiPage instance given a title and text.
     *
     * @param title the title of the Wikipedia page.
     * @param text the text of the Wikipedia page.
     */
    public WikiPage(String title, String text) {
        this.title = title;
        this.text = text;
    }

    /**
     * @return the title of this.
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return the text of this.
     */
    public String getText() {
        return text;
    }

    /**
     * @return the id of this.
     */
    public String id() {
        return getTitle();
    }
}
